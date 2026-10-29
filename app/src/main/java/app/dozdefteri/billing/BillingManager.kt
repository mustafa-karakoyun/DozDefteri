package app.dozdefteri.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import app.dozdefteri.core.di.ApplicationScope
import app.dozdefteri.data.prefs.SettingsRepository
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

sealed interface PriceState {
    data object Loading : PriceState
    data class Available(val formattedPrice: String) : PriceState
    data object Unavailable : PriceState
}

enum class BillingMessage { PURCHASED, PENDING, RESTORED, NOTHING_TO_RESTORE, ERROR }

/**
 * One-time "pro_lifetime" purchase through Google Play Billing. There is no server-side verification; the purchase
 * state is cached in DataStore (`pro_cached`) so Pro works offline and survives Play being unreachable.
 */
@Singleton
class BillingManager @Inject constructor(
    @ApplicationContext context: Context,
    private val settingsRepository: SettingsRepository,
    proStatus: ProStatus,
    @ApplicationScope private val scope: CoroutineScope
) : PurchasesUpdatedListener {

    /** Starts from the cached value; `DEBUG_FORCE_PRO` keeps it true. */
    val isPro: StateFlow<Boolean> = proStatus.isPro

    private val _price = MutableStateFlow<PriceState>(PriceState.Loading)
    val price: StateFlow<PriceState> = _price.asStateFlow()

    private val _messages = MutableSharedFlow<BillingMessage>(extraBufferCapacity = 4)
    val messages: SharedFlow<BillingMessage> = _messages.asSharedFlow()

    private val client: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    private var productDetails: ProductDetails? = null
    private var retryCount = 0
    private var connecting = false

    /** Connects (once) and refreshes purchases and the price. Safe to call repeatedly. */
    fun start() {
        if (client.isReady) {
            scope.launch { refresh() }
            return
        }
        connect()
    }

    private fun connect() {
        if (connecting) return
        connecting = true
        try {
            client.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    connecting = false
                    if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                        retryCount = 0
                        scope.launch { refresh() }
                    } else {
                        Log.w(TAG, "Billing setup failed: ${result.responseCode}")
                        _price.value = PriceState.Unavailable
                        scheduleReconnect()
                    }
                }

                override fun onBillingServiceDisconnected() {
                    connecting = false
                    scheduleReconnect()
                }
            })
        } catch (e: IllegalStateException) {
            connecting = false
            Log.w(TAG, "Billing connection could not start", e)
            _price.value = PriceState.Unavailable
        }
    }

    /** Exponential back-off: 1, 2, 4, 8 seconds, at most 4 attempts. */
    private fun scheduleReconnect() {
        if (retryCount >= MAX_RETRIES) {
            if (_price.value == PriceState.Loading) _price.value = PriceState.Unavailable
            return
        }
        val delayMillis = INITIAL_RETRY_MILLIS shl retryCount
        retryCount++
        scope.launch {
            delay(delayMillis)
            connect()
        }
    }

    private suspend fun refresh() {
        refreshPurchases()
        loadProductDetails()
    }

    /**
     * Queries owned in-app purchases. Pro is turned on for a PURCHASED `pro_lifetime` and off when a successful query
     * finds none; a failed query keeps the cached value. Returns whether the query succeeded.
     */
    private suspend fun refreshPurchases(): Boolean {
        val params = QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
        val (result, purchases) = suspendCancellableCoroutine<Pair<BillingResult, List<Purchase>>> { continuation ->
            client.queryPurchasesAsync(params) { billingResult, list ->
                if (continuation.isActive) continuation.resume(billingResult to list)
            }
        }
        if (result.responseCode != BillingClient.BillingResponseCode.OK) return false
        val pro = purchases.filter {
            ProStatus.PRODUCT_ID in it.products && it.purchaseState == Purchase.PurchaseState.PURCHASED
        }
        pro.filterNot { it.isAcknowledged }.forEach { acknowledge(it) }
        settingsRepository.setProCached(pro.isNotEmpty())
        return true
    }

    private suspend fun loadProductDetails() {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(ProStatus.PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                )
            )
            .build()
        val details = suspendCancellableCoroutine<ProductDetails?> { continuation ->
            client.queryProductDetailsAsync(params) { billingResult, queryResult ->
                val list = if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryResult.productDetailsList
                } else {
                    emptyList()
                }
                val match = list.firstOrNull { it.productId == ProStatus.PRODUCT_ID }
                if (continuation.isActive) continuation.resume(match)
            }
        }
        productDetails = details
        val formatted = details?.oneTimePurchaseOfferDetails?.formattedPrice
        _price.value = if (formatted != null) PriceState.Available(formatted) else PriceState.Unavailable
    }

    private suspend fun acknowledge(purchase: Purchase) {
        val params = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
        suspendCancellableCoroutine<Unit> { continuation ->
            client.acknowledgePurchase(params) { result ->
                if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                    Log.w(TAG, "Acknowledge failed: ${result.responseCode}")
                }
                if (continuation.isActive) continuation.resume(Unit)
            }
        }
    }

    /** Opens the Play purchase sheet. Returns false when the product is not loaded yet. */
    fun launchPurchase(activity: Activity): Boolean {
        val details = productDetails ?: return false
        if (!client.isReady) return false
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(details).build())
            )
            .build()
        val result = client.launchBillingFlow(activity, params)
        return result.responseCode == BillingClient.BillingResponseCode.OK
    }

    /** "Restore purchase": re-queries Play and reports the outcome through [messages]. */
    fun restorePurchases() {
        if (!client.isReady) {
            retryCount = 0
            connect()
            _messages.tryEmit(BillingMessage.ERROR)
            return
        }
        scope.launch {
            val ok = refreshPurchases()
            val owned = settingsRepository.current().proCached
            _messages.emit(
                when {
                    !ok -> BillingMessage.ERROR
                    owned -> BillingMessage.RESTORED
                    else -> BillingMessage.NOTHING_TO_RESTORE
                }
            )
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        scope.launch {
            when (result.responseCode) {
                BillingClient.BillingResponseCode.OK -> purchases.orEmpty()
                    .filter { ProStatus.PRODUCT_ID in it.products }
                    .forEach { handlePurchase(it) }
                BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                    refreshPurchases()
                    _messages.emit(BillingMessage.RESTORED)
                }
                BillingClient.BillingResponseCode.USER_CANCELED -> Unit
                else -> _messages.emit(BillingMessage.ERROR)
            }
        }
    }

    private suspend fun handlePurchase(purchase: Purchase) {
        when (purchase.purchaseState) {
            Purchase.PurchaseState.PURCHASED -> {
                if (!purchase.isAcknowledged) acknowledge(purchase)
                settingsRepository.setProCached(true)
                _messages.emit(BillingMessage.PURCHASED)
            }
            Purchase.PurchaseState.PENDING -> _messages.emit(BillingMessage.PENDING)
            else -> Unit
        }
    }

    private companion object {
        const val TAG = "BillingManager"
        const val MAX_RETRIES = 4
        const val INITIAL_RETRY_MILLIS = 1_000L
    }
}

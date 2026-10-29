package app.dozdefteri.ui.pro

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.dozdefteri.billing.BillingManager
import app.dozdefteri.billing.BillingMessage
import app.dozdefteri.billing.PriceState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class ProUiState(
    val isPro: Boolean = false,
    val price: PriceState = PriceState.Loading
)

@HiltViewModel
class ProViewModel @Inject constructor(
    private val billingManager: BillingManager
) : ViewModel() {

    val uiState: StateFlow<ProUiState> = combine(billingManager.isPro, billingManager.price) { pro, price ->
        ProUiState(isPro = pro, price = price)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProUiState())

    val messages: SharedFlow<BillingMessage> = billingManager.messages

    init {
        billingManager.start()
    }

    /** The activity is only passed through to Play Billing for this call and never stored. */
    fun purchase(activity: Activity): Boolean = billingManager.launchPurchase(activity)

    fun restore() = billingManager.restorePurchases()
}

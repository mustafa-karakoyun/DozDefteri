package app.dozdefteri.alarm

import android.content.BroadcastReceiver
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

private const val RECEIVER_TIMEOUT_MILLIS = 9_000L

private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

/**
 * Runs [block] off the main thread while keeping the broadcast alive with [BroadcastReceiver.goAsync].
 * The work is capped below the 10-second broadcast limit and failures are logged instead of crashing.
 */
fun BroadcastReceiver.runAsync(tag: String, block: suspend () -> Unit) {
    val pendingResult = goAsync()
    receiverScope.launch {
        try {
            withTimeout(RECEIVER_TIMEOUT_MILLIS) { block() }
        } catch (e: Exception) {
            Log.e(tag, "Receiver work failed", e)
        } finally {
            pendingResult.finish()
        }
    }
}

package app.dozdefteri.data.repository

import app.dozdefteri.data.db.entity.MedicationEntity

/** Shows the low-stock alert. Implemented in the alarm package so repositories stay free of Android UI classes. */
fun interface LowStockNotifier {
    /** [remainingDays] is the estimated number of days the stock lasts, or null when it cannot be estimated. */
    fun notifyLowStock(medication: MedicationEntity, remainingDays: Int?)
}

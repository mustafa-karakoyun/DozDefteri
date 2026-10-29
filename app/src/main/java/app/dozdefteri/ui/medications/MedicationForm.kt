package app.dozdefteri.ui.medications

import app.dozdefteri.domain.model.MedicationForm
import app.dozdefteri.domain.model.ScheduleType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/** Raw, editable values of the add/edit medication form. Numbers stay as text until validated. */
data class MedicationFormState(
    val name: String = "",
    val doseAmount: String = "1",
    val doseUnit: String = "",
    val form: MedicationForm = MedicationForm.TABLET,
    val scheduleType: ScheduleType = ScheduleType.DAILY,
    val times: List<LocalTime> = listOf(LocalTime.of(8, 0)),
    val days: Set<DayOfWeek> = emptySet(),
    val intervalDays: Int = MedicationFormValidator.MIN_INTERVAL,
    val startDate: LocalDate,
    val endDate: LocalDate? = null,
    val stockEnabled: Boolean = false,
    val stockQuantity: String = "",
    val lowStockThreshold: String = "",
    val note: String = ""
)

enum class FieldError {
    NAME_REQUIRED,
    NAME_TOO_LONG,
    AMOUNT_INVALID,
    UNIT_REQUIRED,
    UNIT_TOO_LONG,
    TIMES_REQUIRED,
    TIMES_TOO_MANY,
    DAYS_REQUIRED,
    INTERVAL_RANGE,
    END_BEFORE_START,
    STOCK_INVALID,
    THRESHOLD_INVALID,
    NOTE_TOO_LONG
}

data class MedicationFormErrors(
    val name: FieldError? = null,
    val doseAmount: FieldError? = null,
    val doseUnit: FieldError? = null,
    val times: FieldError? = null,
    val days: FieldError? = null,
    val interval: FieldError? = null,
    val endDate: FieldError? = null,
    val stockQuantity: FieldError? = null,
    val lowStockThreshold: FieldError? = null,
    val note: FieldError? = null
) {
    val isValid: Boolean
        get() = listOf(
            name, doseAmount, doseUnit, times, days, interval, endDate, stockQuantity, lowStockThreshold, note
        ).all { it == null }
}

/** Validation rules of plan section 6.4. Pure Kotlin. */
object MedicationFormValidator {
    const val NAME_MAX = 60
    const val UNIT_MAX = 20
    const val NOTE_MAX = 200
    const val TIMES_MAX = 12
    const val MIN_INTERVAL = 2
    const val MAX_INTERVAL = 30

    private val decimalPattern = Regex("""^\d+([.,]\d{1,2})?$""")

    /** Parses a non-negative number with at most two decimals, accepting both "." and "," as separator. */
    fun parseDecimal(text: String): Double? {
        val trimmed = text.trim()
        if (!decimalPattern.matches(trimmed)) return null
        return trimmed.replace(',', '.').toDoubleOrNull()
    }

    fun validate(state: MedicationFormState): MedicationFormErrors {
        val name = state.name.trim()
        val unit = state.doseUnit.trim()
        val amount = parseDecimal(state.doseAmount)
        val needsTimes = state.scheduleType != ScheduleType.AS_NEEDED
        return MedicationFormErrors(
            name = when {
                name.isEmpty() -> FieldError.NAME_REQUIRED
                name.length > NAME_MAX -> FieldError.NAME_TOO_LONG
                else -> null
            },
            doseAmount = if (amount == null || amount <= 0.0) FieldError.AMOUNT_INVALID else null,
            doseUnit = when {
                unit.isEmpty() -> FieldError.UNIT_REQUIRED
                unit.length > UNIT_MAX -> FieldError.UNIT_TOO_LONG
                else -> null
            },
            times = when {
                !needsTimes -> null
                state.times.isEmpty() -> FieldError.TIMES_REQUIRED
                state.times.distinct().size > TIMES_MAX -> FieldError.TIMES_TOO_MANY
                else -> null
            },
            days = if (state.scheduleType == ScheduleType.WEEKDAYS && state.days.isEmpty()) {
                FieldError.DAYS_REQUIRED
            } else {
                null
            },
            interval = if (state.scheduleType == ScheduleType.EVERY_N_DAYS &&
                state.intervalDays !in MIN_INTERVAL..MAX_INTERVAL
            ) {
                FieldError.INTERVAL_RANGE
            } else {
                null
            },
            endDate = if (state.endDate != null && state.endDate.isBefore(state.startDate)) {
                FieldError.END_BEFORE_START
            } else {
                null
            },
            stockQuantity = if (state.stockEnabled && parseDecimal(state.stockQuantity) == null) {
                FieldError.STOCK_INVALID
            } else {
                null
            },
            lowStockThreshold = if (state.stockEnabled && state.lowStockThreshold.isNotBlank() &&
                parseDecimal(state.lowStockThreshold) == null
            ) {
                FieldError.THRESHOLD_INVALID
            } else {
                null
            },
            note = if (state.note.trim().length > NOTE_MAX) FieldError.NOTE_TOO_LONG else null
        )
    }
}

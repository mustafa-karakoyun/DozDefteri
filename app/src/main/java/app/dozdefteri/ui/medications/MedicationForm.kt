package app.dozdefteri.ui.medications

import app.dozdefteri.domain.model.MedicationForm
import app.dozdefteri.domain.model.ScheduleType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/** Raw, editable values of the add/edit medication screen. Numbers stay as text until validated. */
data class MedicationFormState(
    val name: String = "",
    /** Strength per unit ("Miktar"), optional, e.g. "500". */
    val strengthAmount: String = "",
    val strengthUnit: String = MedicationFormValidator.STRENGTH_UNITS.first(),
    val form: MedicationForm = MedicationForm.TABLET,
    /** Amount taken each time, in form units (1 tablet, 2 damla). */
    val perDose: String = "1",
    val scheduleType: ScheduleType = ScheduleType.DAILY,
    val times: List<LocalTime> = listOf(LocalTime.of(8, 0)),
    val days: Set<DayOfWeek> = emptySet(),
    val intervalDays: String = "3",
    val startDate: LocalDate,
    val durationDays: String = MedicationFormValidator.DEFAULT_DURATION.toString(),
    val indefinite: Boolean = false,
    /** Current stock; blank means stock is not tracked. */
    val stockQuantity: String = "",
    val lowStockDays: String = MedicationFormValidator.DEFAULT_LOW_STOCK_DAYS.toString(),
    val note: String = ""
) {
    val endDate: LocalDate?
        get() = if (indefinite) null else durationDays.trim().toIntOrNull()?.let { startDate.plusDays(it - 1L) }
}

enum class FieldError {
    NAME_REQUIRED,
    NAME_TOO_LONG,
    STRENGTH_INVALID,
    PER_DOSE_INVALID,
    TIMES_REQUIRED,
    TIMES_TOO_MANY,
    DAYS_REQUIRED,
    INTERVAL_RANGE,
    DURATION_INVALID,
    STOCK_INVALID,
    LOW_STOCK_DAYS_INVALID,
    NOTE_TOO_LONG
}

/** Form fields in screen order, used to scroll to the first error. */
enum class FormField { NAME, STRENGTH, PER_DOSE, SCHEDULE, TIMES, DURATION, STOCK, NOTE }

data class MedicationFormErrors(
    val name: FieldError? = null,
    val strength: FieldError? = null,
    val perDose: FieldError? = null,
    val times: FieldError? = null,
    val days: FieldError? = null,
    val interval: FieldError? = null,
    val duration: FieldError? = null,
    val stock: FieldError? = null,
    val lowStockDays: FieldError? = null,
    val note: FieldError? = null
) {
    val isValid: Boolean get() = firstInvalid == null

    val firstInvalid: FormField?
        get() = when {
            name != null -> FormField.NAME
            strength != null -> FormField.STRENGTH
            perDose != null -> FormField.PER_DOSE
            days != null || interval != null -> FormField.SCHEDULE
            times != null -> FormField.TIMES
            duration != null -> FormField.DURATION
            stock != null || lowStockDays != null -> FormField.STOCK
            note != null -> FormField.NOTE
            else -> null
        }
}

/** Validation rules of the medication screen. Pure Kotlin. */
object MedicationFormValidator {
    const val NAME_MAX = 60
    const val NOTE_MAX = 200
    const val TIMES_MAX = 12
    const val MIN_INTERVAL = 2
    const val MAX_INTERVAL = 30
    const val MAX_DURATION = 3650
    const val MAX_LOW_STOCK_DAYS = 365
    const val DEFAULT_DURATION = 30
    const val DEFAULT_LOW_STOCK_DAYS = 7
    val STRENGTH_UNITS = listOf("mg", "mcg", "g", "ml", "IU", "%")
    val SUGGESTED_TIMES: List<LocalTime> = listOf(LocalTime.of(8, 0), LocalTime.of(13, 0), LocalTime.of(20, 0))

    private val decimalPattern = Regex("""^\d+([.,]\d{1,2})?$""")

    /** Parses a non-negative number with at most two decimals, accepting both "." and "," as separator. */
    fun parseDecimal(text: String): Double? {
        val trimmed = text.trim()
        if (!decimalPattern.matches(trimmed)) return null
        return trimmed.replace(',', '.').toDoubleOrNull()
    }

    fun validate(state: MedicationFormState): MedicationFormErrors {
        val name = state.name.trim()
        val needsTimes = state.scheduleType != ScheduleType.AS_NEEDED
        val interval = state.intervalDays.trim().toIntOrNull()
        val duration = state.durationDays.trim().toIntOrNull()
        val tracksStock = state.stockQuantity.isNotBlank()
        val lowDays = state.lowStockDays.trim()
        return MedicationFormErrors(
            name = when {
                name.isEmpty() -> FieldError.NAME_REQUIRED
                name.length > NAME_MAX -> FieldError.NAME_TOO_LONG
                else -> null
            },
            strength = if (state.strengthAmount.isNotBlank() && (parseDecimal(state.strengthAmount) ?: 0.0) <= 0.0) {
                FieldError.STRENGTH_INVALID
            } else {
                null
            },
            perDose = if ((parseDecimal(state.perDose) ?: 0.0) <= 0.0) FieldError.PER_DOSE_INVALID else null,
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
                (interval == null || interval !in MIN_INTERVAL..MAX_INTERVAL)
            ) {
                FieldError.INTERVAL_RANGE
            } else {
                null
            },
            duration = if (!state.indefinite && (duration == null || duration !in 1..MAX_DURATION)) {
                FieldError.DURATION_INVALID
            } else {
                null
            },
            stock = if (tracksStock && parseDecimal(state.stockQuantity) == null) FieldError.STOCK_INVALID else null,
            lowStockDays = if (tracksStock && lowDays.isNotEmpty() &&
                (lowDays.toIntOrNull() ?: -1) !in 0..MAX_LOW_STOCK_DAYS
            ) {
                FieldError.LOW_STOCK_DAYS_INVALID
            } else {
                null
            },
            note = if (state.note.trim().length > NOTE_MAX) FieldError.NOTE_TOO_LONG else null
        )
    }
}

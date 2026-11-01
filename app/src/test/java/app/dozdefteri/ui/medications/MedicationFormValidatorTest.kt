package app.dozdefteri.ui.medications

import app.dozdefteri.domain.model.ScheduleType
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

class MedicationFormValidatorTest {

    private val valid =
        MedicationFormState(name = "Metformin", strengthAmount = "500", startDate = LocalDate.of(2026, 11, 1))

    private fun errors(state: MedicationFormState) = MedicationFormValidator.validate(state)

    @Test
    fun smartDefaults_withAName_areValid() {
        val defaults = MedicationFormState(name = "D vitamini", startDate = LocalDate.of(2026, 11, 1))
        assertThat(errors(defaults).isValid).isTrue()
        assertThat(valid.endDate).isEqualTo(LocalDate.of(2026, 11, 30))
    }

    @Test
    fun name_requiredAndLimited() {
        assertThat(errors(valid.copy(name = "  ")).name).isEqualTo(FieldError.NAME_REQUIRED)
        assertThat(errors(valid.copy(name = "a".repeat(61))).name).isEqualTo(FieldError.NAME_TOO_LONG)
        assertThat(errors(valid.copy(name = "  ")).firstInvalid).isEqualTo(FormField.NAME)
    }

    @Test
    fun strength_isOptional_butPositiveWhenGiven() {
        assertThat(errors(valid.copy(strengthAmount = "")).strength).isNull()
        assertThat(errors(valid.copy(strengthAmount = "0")).strength).isEqualTo(FieldError.STRENGTH_INVALID)
        assertThat(errors(valid.copy(strengthAmount = "abc")).strength).isEqualTo(FieldError.STRENGTH_INVALID)
        assertThat(errors(valid.copy(strengthAmount = "0,5")).strength).isNull()
    }

    @Test
    fun perDose_mustBePositive() {
        assertThat(errors(valid.copy(perDose = "0")).perDose).isEqualTo(FieldError.PER_DOSE_INVALID)
        assertThat(errors(valid.copy(perDose = "0.5")).perDose).isNull()
    }

    @Test
    fun times_requiredExceptAsNeeded() {
        assertThat(errors(valid.copy(times = emptyList())).times).isEqualTo(FieldError.TIMES_REQUIRED)
        assertThat(errors(valid.copy(times = emptyList(), scheduleType = ScheduleType.AS_NEEDED)).isValid).isTrue()
        val thirteen = (0..12).map { LocalTime.of(it, 0) }
        assertThat(errors(valid.copy(times = thirteen)).times).isEqualTo(FieldError.TIMES_TOO_MANY)
    }

    @Test
    fun weekdays_andInterval() {
        assertThat(errors(valid.copy(scheduleType = ScheduleType.WEEKDAYS)).days).isEqualTo(FieldError.DAYS_REQUIRED)
        assertThat(errors(valid.copy(scheduleType = ScheduleType.WEEKDAYS, days = setOf(DayOfWeek.MONDAY))).isValid)
            .isTrue()
        val everyN = valid.copy(scheduleType = ScheduleType.EVERY_N_DAYS)
        assertThat(errors(everyN.copy(intervalDays = "1")).interval).isEqualTo(FieldError.INTERVAL_RANGE)
        assertThat(errors(everyN.copy(intervalDays = "31")).interval).isEqualTo(FieldError.INTERVAL_RANGE)
        assertThat(errors(everyN.copy(intervalDays = "3")).isValid).isTrue()
    }

    @Test
    fun duration_orIndefinite() {
        assertThat(errors(valid.copy(durationDays = "0")).duration).isEqualTo(FieldError.DURATION_INVALID)
        assertThat(errors(valid.copy(durationDays = "", indefinite = true)).isValid).isTrue()
        assertThat(valid.copy(indefinite = true).endDate).isNull()
    }

    @Test
    fun stock_optional_lowStockDaysValidatedOnlyWithStock() {
        assertThat(errors(valid.copy(stockQuantity = "", lowStockDays = "x")).isValid).isTrue()
        assertThat(errors(valid.copy(stockQuantity = "-1")).stock).isEqualTo(FieldError.STOCK_INVALID)
        assertThat(errors(valid.copy(stockQuantity = "38", lowStockDays = "400")).lowStockDays)
            .isEqualTo(FieldError.LOW_STOCK_DAYS_INVALID)
        assertThat(errors(valid.copy(stockQuantity = "38", lowStockDays = "")).isValid).isTrue()
    }

    @Test
    fun note_limited() {
        assertThat(errors(valid.copy(note = "n".repeat(201))).note).isEqualTo(FieldError.NOTE_TOO_LONG)
    }
}

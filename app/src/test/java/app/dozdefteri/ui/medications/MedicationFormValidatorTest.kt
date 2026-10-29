package app.dozdefteri.ui.medications

import app.dozdefteri.domain.model.ScheduleType
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

class MedicationFormValidatorTest {

    private val valid = MedicationFormState(
        name = "Vitamin D",
        doseAmount = "1",
        doseUnit = "tablet",
        startDate = LocalDate.of(2026, 11, 1)
    )

    @Test
    fun validForm_hasNoErrors() {
        assertThat(MedicationFormValidator.validate(valid).isValid).isTrue()
    }

    @Test
    fun name_isTrimmed_andLimitedTo60() {
        assertThat(MedicationFormValidator.validate(valid.copy(name = "   ")).name).isEqualTo(FieldError.NAME_REQUIRED)
        assertThat(MedicationFormValidator.validate(valid.copy(name = "a".repeat(61))).name)
            .isEqualTo(FieldError.NAME_TOO_LONG)
        assertThat(MedicationFormValidator.validate(valid.copy(name = " " + "a".repeat(60) + " ")).name).isNull()
    }

    @Test
    fun amount_mustBePositive_withAtMostTwoDecimals() {
        listOf("0", "-1", "1.234", "abc", "").forEach {
            assertThat(MedicationFormValidator.validate(valid.copy(doseAmount = it)).doseAmount)
                .isEqualTo(FieldError.AMOUNT_INVALID)
        }
        listOf("0.5", "0,5", "2", "1.25").forEach {
            assertThat(MedicationFormValidator.validate(valid.copy(doseAmount = it)).doseAmount).isNull()
        }
        assertThat(MedicationFormValidator.parseDecimal("0,5")).isEqualTo(0.5)
    }

    @Test
    fun unit_isRequired_andLimitedTo20() {
        assertThat(MedicationFormValidator.validate(valid.copy(doseUnit = "")).doseUnit).isEqualTo(FieldError.UNIT_REQUIRED)
        assertThat(MedicationFormValidator.validate(valid.copy(doseUnit = "x".repeat(21))).doseUnit)
            .isEqualTo(FieldError.UNIT_TOO_LONG)
    }

    @Test
    fun times_requiredExceptAsNeeded_andAtMost12() {
        assertThat(MedicationFormValidator.validate(valid.copy(times = emptyList())).times)
            .isEqualTo(FieldError.TIMES_REQUIRED)
        assertThat(
            MedicationFormValidator.validate(valid.copy(times = emptyList(), scheduleType = ScheduleType.AS_NEEDED)).isValid
        ).isTrue()
        val thirteen = (0..12).map { LocalTime.of(it, 0) }
        assertThat(MedicationFormValidator.validate(valid.copy(times = thirteen)).times)
            .isEqualTo(FieldError.TIMES_TOO_MANY)
    }

    @Test
    fun weekdays_needAtLeastOneDay() {
        val weekdays = valid.copy(scheduleType = ScheduleType.WEEKDAYS)
        assertThat(MedicationFormValidator.validate(weekdays).days).isEqualTo(FieldError.DAYS_REQUIRED)
        assertThat(MedicationFormValidator.validate(weekdays.copy(days = setOf(DayOfWeek.MONDAY))).days).isNull()
    }

    @Test
    fun interval_between2And30() {
        val everyN = valid.copy(scheduleType = ScheduleType.EVERY_N_DAYS)
        assertThat(MedicationFormValidator.validate(everyN.copy(intervalDays = 1)).interval)
            .isEqualTo(FieldError.INTERVAL_RANGE)
        assertThat(MedicationFormValidator.validate(everyN.copy(intervalDays = 31)).interval)
            .isEqualTo(FieldError.INTERVAL_RANGE)
        assertThat(MedicationFormValidator.validate(everyN.copy(intervalDays = 30)).interval).isNull()
    }

    @Test
    fun endDate_cannotBeBeforeStart() {
        assertThat(MedicationFormValidator.validate(valid.copy(endDate = LocalDate.of(2026, 10, 31))).endDate)
            .isEqualTo(FieldError.END_BEFORE_START)
        assertThat(MedicationFormValidator.validate(valid.copy(endDate = LocalDate.of(2026, 11, 1))).endDate).isNull()
    }

    @Test
    fun stock_validatedOnlyWhenEnabled() {
        assertThat(MedicationFormValidator.validate(valid.copy(stockQuantity = "x")).isValid).isTrue()
        val enabled = valid.copy(stockEnabled = true)
        assertThat(MedicationFormValidator.validate(enabled.copy(stockQuantity = "")).stockQuantity)
            .isEqualTo(FieldError.STOCK_INVALID)
        assertThat(MedicationFormValidator.validate(enabled.copy(stockQuantity = "0")).stockQuantity).isNull()
        assertThat(MedicationFormValidator.validate(enabled.copy(stockQuantity = "5", lowStockThreshold = "-1")).lowStockThreshold)
            .isEqualTo(FieldError.THRESHOLD_INVALID)
        assertThat(MedicationFormValidator.validate(enabled.copy(stockQuantity = "5", lowStockThreshold = "")).isValid).isTrue()
    }

    @Test
    fun note_atMost200() {
        assertThat(MedicationFormValidator.validate(valid.copy(note = "n".repeat(201))).note)
            .isEqualTo(FieldError.NOTE_TOO_LONG)
    }
}

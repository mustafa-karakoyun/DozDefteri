package app.dozdefteri.domain

import app.dozdefteri.domain.model.DoseStatus
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DayStatusCalculatorTest {
    @Test
    fun emptyOrOnlyWaiting_isNone() {
        assertThat(DayStatusCalculator.calculate(emptyList())).isEqualTo(DayStatus.NONE)
        assertThat(DayStatusCalculator.calculate(listOf(DoseStatus.PENDING, DoseStatus.SNOOZED)))
            .isEqualTo(DayStatus.NONE)
    }

    @Test
    fun allTaken() {
        assertThat(DayStatusCalculator.calculate(listOf(DoseStatus.TAKEN, DoseStatus.TAKEN)))
            .isEqualTo(DayStatus.ALL_TAKEN)
    }

    @Test
    fun anyMissed_winsOverEverything() {
        assertThat(DayStatusCalculator.calculate(listOf(DoseStatus.TAKEN, DoseStatus.MISSED, DoseStatus.SKIPPED)))
            .isEqualTo(DayStatus.MISSED)
    }

    @Test
    fun skippedOrMixed_isPartial() {
        assertThat(DayStatusCalculator.calculate(listOf(DoseStatus.TAKEN, DoseStatus.SKIPPED)))
            .isEqualTo(DayStatus.PARTIAL)
        assertThat(DayStatusCalculator.calculate(listOf(DoseStatus.TAKEN, DoseStatus.PENDING)))
            .isEqualTo(DayStatus.PARTIAL)
    }
}

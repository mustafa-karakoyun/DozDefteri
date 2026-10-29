package app.dozdefteri.domain

import app.dozdefteri.data.db.entity.DoseEventEntity
import app.dozdefteri.domain.model.DoseStatus
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AdherenceCalculatorTest {

    private val calculator = AdherenceCalculator()

    private fun event(status: DoseStatus, scheduleId: Long = 1, time: Long = 0) = DoseEventEntity(
        id = 0,
        scheduleId = scheduleId,
        medicationId = 1,
        profileId = 1,
        originalScheduledAt = time,
        scheduledAt = time,
        status = status,
        notifiedAt = null,
        actionAt = null,
        quantity = 1.0
    )

    @Test
    fun allTaken_is100() {
        val events = List(4) { event(DoseStatus.TAKEN, time = it.toLong()) }
        assertThat(calculator.calculate(events)).isEqualTo(100)
    }

    @Test
    fun mixed_isRounded_andIgnoresOpenStatuses() {
        val events = listOf(
            event(DoseStatus.TAKEN),
            event(DoseStatus.TAKEN),
            event(DoseStatus.SKIPPED),
            event(DoseStatus.PENDING),
            event(DoseStatus.SNOOZED)
        )
        // 2 / 3 = 66.67 -> 67
        assertThat(calculator.calculate(events)).isEqualTo(67)

        val withMissed = listOf(event(DoseStatus.TAKEN), event(DoseStatus.MISSED))
        assertThat(calculator.calculate(withMissed)).isEqualTo(50)
    }

    @Test
    fun zeroDenominator_isNull() {
        assertThat(calculator.calculate(emptyList())).isNull()
        assertThat(calculator.calculate(listOf(event(DoseStatus.PENDING)))).isNull()
    }

    @Test
    fun asNeededEvents_areExcluded() {
        val events = listOf(
            event(DoseStatus.TAKEN, scheduleId = 1),
            event(DoseStatus.MISSED, scheduleId = 1),
            event(DoseStatus.TAKEN, scheduleId = 2),
            event(DoseStatus.TAKEN, scheduleId = 2)
        )
        assertThat(calculator.calculate(events, asNeededScheduleIds = setOf(2L))).isEqualTo(50)
        assertThat(calculator.calculate(events.drop(2), asNeededScheduleIds = setOf(2L))).isNull()
    }
}

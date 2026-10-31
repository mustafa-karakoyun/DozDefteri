package app.dozdefteri.domain

import app.dozdefteri.domain.model.DoseStatus
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class HistoryStatsTest {
    @Test
    fun designExample_148Planned_136Taken_is92Percent() {
        val statuses = List(136) { DoseStatus.TAKEN } + List(5) { DoseStatus.SKIPPED } + List(7) { DoseStatus.MISSED }
        val counts = HistoryStats.count(statuses)
        assertThat(counts.planned).isEqualTo(148)
        assertThat(counts.percent).isEqualTo(92)
    }

    @Test
    fun waitingDosesAreNotPlannedYet() {
        val counts = HistoryStats.count(listOf(DoseStatus.TAKEN, DoseStatus.PENDING, DoseStatus.SNOOZED))
        assertThat(counts.planned).isEqualTo(1)
        assertThat(counts.total).isEqualTo(3)
        assertThat(counts.percent).isEqualTo(100)
    }

    @Test
    fun nothingFinal_hasNoPercent() {
        assertThat(HistoryStats.count(emptyList()).percent).isNull()
        assertThat(HistoryStats.count(listOf(DoseStatus.PENDING)).percent).isNull()
    }
}

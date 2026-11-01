package app.dozdefteri.ui.today

import app.dozdefteri.data.db.dao.DoseEventDetails
import app.dozdefteri.data.db.entity.DoseEventEntity
import app.dozdefteri.data.db.entity.MedicationEntity
import app.dozdefteri.domain.model.DoseStatus
import app.dozdefteri.domain.model.MedicationForm
import app.dozdefteri.domain.model.ScheduleType
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalTime

class TodayGroupingTest {

    private fun dose(
        medicationId: Long,
        time: String,
        status: DoseStatus = DoseStatus.PENDING,
        snoozedTo: String? = null
    ) =
        TodayDose(
            details = DoseEventDetails(
                event = DoseEventEntity(
                    id = medicationId * 100 + LocalTime.parse(time).hour,
                    scheduleId = medicationId,
                    medicationId = medicationId,
                    profileId = 1,
                    originalScheduledAt = 0,
                    scheduledAt = 0,
                    status = status,
                    notifiedAt = null,
                    actionAt = null,
                    quantity = 1.0
                ),
                medicationName = "Med $medicationId",
                doseAmount = 1.0,
                doseUnit = "tablet",
                scheduleType = ScheduleType.DAILY,
                profileName = "Ayşe"
            ),
            plannedTime = LocalTime.parse(time),
            scheduledTime = LocalTime.parse(snoozedTo ?: time),
            takenTime = null,
            canUndo = false
        )

    private fun lowStock(id: Long) = LowStockItem(
        MedicationEntity(
            id = id,
            profileId = 1,
            name = "Med $id",
            doseAmount = 1.0,
            doseUnit = "tablet",
            form = MedicationForm.TABLET,
            note = null,
            stockQuantity = 2.0,
            lowStockThreshold = 5.0,
            createdAt = 0,
            updatedAt = 0
        ),
        remainingDays = 2,
        fraction = 0.1f
    )

    @Test
    fun dayParts_useThePlannedTimeBoundaries() {
        assertThat(DayPart.of(LocalTime.of(11, 59))).isEqualTo(DayPart.MORNING)
        assertThat(DayPart.of(LocalTime.of(12, 0))).isEqualTo(DayPart.NOON)
        assertThat(DayPart.of(LocalTime.of(16, 59))).isEqualTo(DayPart.NOON)
        assertThat(DayPart.of(LocalTime.of(17, 0))).isEqualTo(DayPart.EVENING)
    }

    @Test
    fun emptyGroupsAreHidden_andDosesSortedByTime() {
        val groups = TodayGrouping.build(listOf(dose(2, "20:00"), dose(1, "07:30"), dose(3, "08:00")), emptyList())
        assertThat(groups.map { it.part }).containsExactly(DayPart.MORNING, DayPart.EVENING).inOrder()
        assertThat(groups.first().doses.map { it.plannedTime })
            .containsExactly(LocalTime.of(7, 30), LocalTime.of(8, 0)).inOrder()
    }

    @Test
    fun lowStockCard_followsTheLastGroupOfItsMedication() {
        val groups = TodayGrouping.build(
            listOf(dose(1, "08:00"), dose(1, "20:00"), dose(2, "13:00")),
            listOf(lowStock(1), lowStock(9))
        )
        val evening = groups.single { it.part == DayPart.EVENING }
        assertThat(evening.lowStock.map { it.medication.id }).containsExactly(1L, 9L)
        assertThat(groups.single { it.part == DayPart.MORNING }.lowStock).isEmpty()
    }

    @Test
    fun next_isTheEarliestOpenDose_usingTheSnoozedTime() {
        val next = TodayGrouping.next(
            listOf(
                dose(1, "08:00", DoseStatus.TAKEN),
                dose(2, "13:00", DoseStatus.SNOOZED, snoozedTo = "13:10"),
                dose(3, "20:00")
            )
        )
        assertThat(next).isEqualTo(NextDose("Med 2", LocalTime.of(13, 10), snoozed = true))
        assertThat(TodayGrouping.next(listOf(dose(1, "08:00", DoseStatus.TAKEN)))).isNull()
    }
}

package app.dozdefteri.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.dozdefteri.R
import app.dozdefteri.data.db.dao.DoseEventDetails
import app.dozdefteri.data.db.entity.DoseEventEntity
import app.dozdefteri.domain.model.DoseStatus
import app.dozdefteri.domain.model.ScheduleType
import app.dozdefteri.ui.components.PermissionSnapshot
import app.dozdefteri.ui.theme.DozDefteriTheme
import app.dozdefteri.ui.today.TodayActions
import app.dozdefteri.ui.today.TodayContent
import app.dozdefteri.ui.today.TodayDose
import app.dozdefteri.ui.today.TodayGrouping
import app.dozdefteri.ui.today.TodayUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.LocalTime

@RunWith(AndroidJUnit4::class)
class TodayContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun dose(status: DoseStatus) = TodayDose(
        details = DoseEventDetails(
            event = DoseEventEntity(
                id = 1,
                scheduleId = 1,
                medicationId = 1,
                profileId = 1,
                originalScheduledAt = 0,
                scheduledAt = 0,
                status = status,
                notifiedAt = null,
                actionAt = if (status == DoseStatus.TAKEN) 0 else null,
                quantity = 1.0
            ),
            medicationName = "Metformin",
            doseAmount = 1.0,
            doseUnit = "tablet",
            scheduleType = ScheduleType.DAILY,
            profileName = "Ayşe"
        ),
        plannedTime = LocalTime.of(20, 0),
        scheduledTime = LocalTime.of(20, 0),
        takenTime = if (status == DoseStatus.TAKEN) LocalTime.of(20, 2) else null,
        canUndo = status == DoseStatus.TAKEN
    )

    @Test
    fun takingADose_turnsTheCardIntoTaken() {
        composeRule.setContent {
            var current by remember { mutableStateOf(dose(DoseStatus.PENDING)) }
            DozDefteriTheme {
                TodayContent(
                    state = TodayUiState(
                        isLoading = false,
                        today = LocalDate.of(2026, 11, 1),
                        hasMedications = true,
                        groups = TodayGrouping.build(listOf(current), emptyList()),
                        segments = listOf(current.status)
                    ),
                    header = null,
                    permissions = PermissionSnapshot(notificationsAllowed = true, exactAlarmsAllowed = true),
                    actions = TodayActions(onTake = { current = dose(DoseStatus.TAKEN) })
                )
            }
        }
        composeRule.onNodeWithText(context.getString(R.string.status_pending)).assertExists()
        composeRule.onNodeWithText(context.getString(R.string.action_taken)).performClick()
        composeRule.onNodeWithText(context.getString(R.string.status_taken)).assertExists()
    }
}

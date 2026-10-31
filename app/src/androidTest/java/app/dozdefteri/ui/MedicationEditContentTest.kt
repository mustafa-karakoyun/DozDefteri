package app.dozdefteri.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.dozdefteri.R
import app.dozdefteri.ui.medications.MedicationEditActions
import app.dozdefteri.ui.medications.MedicationEditContent
import app.dozdefteri.ui.medications.MedicationEditUiState
import app.dozdefteri.ui.medications.MedicationFormState
import app.dozdefteri.ui.medications.MedicationFormValidator
import app.dozdefteri.ui.theme.DozDefteriTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class MedicationEditContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun saveWithEmptyName_showsTheNameError() {
        composeRule.setContent {
            var state by remember {
                val form = MedicationFormState(startDate = LocalDate.of(2026, 11, 1))
                mutableStateOf(
                    MedicationEditUiState(
                        isLoading = false,
                        today = LocalDate.of(2026, 11, 1),
                        form = form,
                        errors = MedicationFormValidator.validate(form)
                    )
                )
            }
            DozDefteriTheme {
                MedicationEditContent(
                    state = state,
                    actions = MedicationEditActions(onSave = { state = state.copy(showAllErrors = true) })
                )
            }
        }
        composeRule.onNodeWithText(context.getString(R.string.error_name_required)).assertDoesNotExist()
        composeRule.onAllNodesWithText(context.getString(R.string.save)).onFirst().performClick()
        composeRule.onNodeWithText(context.getString(R.string.error_name_required)).assertExists()
    }
}

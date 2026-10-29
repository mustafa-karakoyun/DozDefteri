package app.dozdefteri.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
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
    fun saveIsDisabledUntilFormIsValid() {
        composeRule.setContent {
            var form by remember { mutableStateOf(MedicationFormState(startDate = LocalDate.of(2026, 11, 1))) }
            val state = MedicationEditUiState(
                isLoading = false,
                form = form,
                errors = MedicationFormValidator.validate(form),
                touched = setOf("name", "doseUnit")
            )
            DozDefteriTheme {
                MedicationEditContent(
                    state = state,
                    actions = MedicationEditActions(
                        onNameChange = { form = form.copy(name = it) },
                        onDoseUnitChange = { form = form.copy(doseUnit = it) }
                    )
                )
            }
        }
        val save = context.getString(R.string.save)
        composeRule.onNodeWithText(save).assertIsNotEnabled()
        composeRule.onNodeWithText(context.getString(R.string.error_name_required)).assertExists()

        composeRule.onNodeWithText(context.getString(R.string.field_name)).performTextInput("Vitamin D")
        composeRule.onNodeWithText(context.getString(R.string.field_dose_unit)).performTextInput("tablet")

        composeRule.onNodeWithText(save).assertIsEnabled()
    }
}

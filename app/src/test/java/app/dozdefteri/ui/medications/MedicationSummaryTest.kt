package app.dozdefteri.ui.medications

import app.dozdefteri.domain.model.ScheduleType
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalTime

class MedicationSummaryTest {

    private val tr = SummaryTexts(
        daily = "Her gün %1\$s",
        weekdays = "%1\$s günleri %2\$s",
        everyNDays = "%1\$d günde bir %2\$s",
        asNeeded = "Gerektiğinde, hatırlatma yok",
        duration = "%1\$d gün boyunca",
        indefinite = "süresiz",
        and = "ve",
        turkish = true
    )
    private val en = SummaryTexts(
        daily = "Every day at %1\$s",
        weekdays = "On %1\$s at %2\$s",
        everyNDays = "Every %1\$d days at %2\$s",
        asNeeded = "As needed, no reminders",
        duration = "for %1\$d days",
        indefinite = "with no end date",
        and = "and",
        turkish = false
    )
    private val eightAndEight = listOf(LocalTime.of(20, 0), LocalTime.of(8, 0))

    @Test
    fun daily_matchesTheDesignSentence() {
        val text = MedicationSummary.build(ScheduleType.DAILY, eightAndEight, emptyList(), null, "1 tablet", 30, tr)
        assertThat(text).isEqualTo("Her gün 08:00 ve 20:00'de, 1 tablet, 30 gün boyunca.")
    }

    @Test
    fun daily_english() {
        val text = MedicationSummary.build(ScheduleType.DAILY, eightAndEight, emptyList(), null, "1 tablet", 30, en)
        assertThat(text).isEqualTo("Every day at 08:00 and 20:00, 1 tablet, for 30 days.")
    }

    @Test
    fun weekdays_threeDays_andSuffixFollowsLastTime() {
        val text = MedicationSummary.build(
            ScheduleType.WEEKDAYS,
            listOf(LocalTime.of(9, 30)),
            listOf("Pzt", "Çar", "Cum"),
            null,
            "2 damla",
            null,
            tr
        )
        assertThat(text).isEqualTo("Pzt, Çar ve Cum günleri 09:30'da, 2 damla, süresiz.")
    }

    @Test
    fun everyNDays_andAsNeeded() {
        assertThat(
            MedicationSummary.build(
                ScheduleType.EVERY_N_DAYS,
                listOf(LocalTime.of(13, 0)),
                emptyList(),
                3,
                "1 kapsül",
                10,
                tr
            )
        ).isEqualTo("3 günde bir 13:00'te, 1 kapsül, 10 gün boyunca.")
        assertThat(
            MedicationSummary.build(ScheduleType.AS_NEEDED, emptyList(), emptyList(), null, "1 tablet", null, tr)
        ).isEqualTo("Gerektiğinde, hatırlatma yok, 1 tablet, süresiz.")
    }

    @Test
    fun timeList_threeTimes() {
        val times = listOf(LocalTime.of(8, 0), LocalTime.of(13, 0), LocalTime.of(20, 0))
        assertThat(MedicationSummary.timeList(times, tr)).isEqualTo("08:00, 13:00 ve 20:00'de")
        assertThat(MedicationSummary.joinList(listOf("a"), "ve")).isEqualTo("a")
    }
}

package app.dozdefteri.core

import app.dozdefteri.core.format.TurkishSuffix
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalTime

class TurkishSuffixTest {

    private fun locative(text: String) = TurkishSuffix.timeLocative(text, LocalTime.parse(text))
    private fun dative(text: String) = TurkishSuffix.timeDative(text, LocalTime.parse(text))

    @Test
    fun locative_followsTheSpokenMinute() {
        assertThat(locative("08:04")).isEqualTo("08:04'te")
        assertThat(locative("07:32")).isEqualTo("07:32'de")
        assertThat(locative("21:15")).isEqualTo("21:15'te")
        assertThat(locative("07:41")).isEqualTo("07:41'de")
        assertThat(locative("20:05")).isEqualTo("20:05'te")
        assertThat(locative("13:30")).isEqualTo("13:30'da")
        assertThat(locative("09:40")).isEqualTo("09:40'ta")
        assertThat(locative("06:26")).isEqualTo("06:26'da")
    }

    @Test
    fun locative_usesTheHourOnFullHours() {
        assertThat(locative("20:00")).isEqualTo("20:00'de")
        assertThat(locative("10:00")).isEqualTo("10:00'da")
        assertThat(locative("03:00")).isEqualTo("03:00'te")
        assertThat(locative("00:00")).isEqualTo("00:00'da")
    }

    @Test
    fun dative() {
        assertThat(dative("20:10")).isEqualTo("20:10'a")
        assertThat(dative("08:02")).isEqualTo("08:02'ye")
        assertThat(dative("13:06")).isEqualTo("13:06'ya")
        assertThat(dative("08:05")).isEqualTo("08:05'e")
        assertThat(dative("08:09")).isEqualTo("08:09'a")
    }

    @Test
    fun genitive() {
        assertThat(TurkishSuffix.nameGenitive("Mehmet")).isEqualTo("Mehmet'in")
        assertThat(TurkishSuffix.nameGenitive("Ayşe")).isEqualTo("Ayşe'nin")
        assertThat(TurkishSuffix.nameGenitive("Zeynep")).isEqualTo("Zeynep'in")
        assertThat(TurkishSuffix.nameGenitive("Oğuz")).isEqualTo("Oğuz'un")
        assertThat(TurkishSuffix.nameGenitive("Ali")).isEqualTo("Ali'nin")
        assertThat(TurkishSuffix.nameGenitive("Ahmet Can")).isEqualTo("Ahmet Can'ın")
        assertThat(TurkishSuffix.nameGenitive("Gül")).isEqualTo("Gül'ün")
    }

    @Test
    fun countPossessive() {
        val tr = java.util.Locale.forLanguageTag("tr")
        assertThat(TurkishSuffix.countPossessive(2, tr)).isEqualTo("2'si")
        assertThat(TurkishSuffix.countPossessive(1, tr)).isEqualTo("1'i")
        assertThat(TurkishSuffix.countPossessive(3, tr)).isEqualTo("3'ü")
        assertThat(TurkishSuffix.countPossessive(9, tr)).isEqualTo("9'u")
        assertThat(TurkishSuffix.countPossessive(136, tr)).isEqualTo("136'sı")
        assertThat(TurkishSuffix.countPossessive(40, tr)).isEqualTo("40'ı")
        assertThat(TurkishSuffix.countPossessive(100, tr)).isEqualTo("100'ü")
        assertThat(TurkishSuffix.countPossessive(0, tr)).isEqualTo("0'ı")
        assertThat(TurkishSuffix.countPossessive(5, java.util.Locale.US)).isEqualTo("5")
    }
}

package app.dozdefteri.core.format

import java.time.LocalTime
import java.util.Locale

/**
 * Turkish case suffixes that follow vowel harmony and consonant hardening, used where the design shows them
 * (e.g. "08:04'te", "20:10'a ertelendi", "Mehmet'in ilaçları"). Pure Kotlin.
 * For times the suffix follows how the time is read aloud: the minute when it is not zero, otherwise the hour.
 */
object TurkishSuffix {

    private val digitWords = listOf("sıfır", "bir", "iki", "üç", "dört", "beş", "altı", "yedi", "sekiz", "dokuz")
    private val tensWords = mapOf(1 to "on", 2 to "yirmi", 3 to "otuz", 4 to "kırk", 5 to "elli")
    private const val FRONT_VOWELS = "eiöü"
    private const val VOWELS = "aeıioöuü"
    private const val HARD_CONSONANTS = "fstkçşhp"

    /** Spoken word whose sound decides the suffix: last non-zero part of the time. */
    fun spokenEnding(time: LocalTime): String {
        val value = if (time.minute != 0) time.minute else time.hour
        return when {
            value == 0 -> digitWords[0]
            value % 10 != 0 -> digitWords[value % 10]
            else -> tensWords[value / 10] ?: digitWords[0]
        }
    }

    /** Locative "-de/-da/-te/-ta": "08:04" -> "te", "07:32" -> "de". */
    fun locative(word: String): String {
        val back = lastVowel(word)?.let { it !in FRONT_VOWELS } ?: false
        val hard = word.lastOrNull()?.lowercaseChar()?.let { it in HARD_CONSONANTS } ?: false
        return (if (hard) "t" else "d") + (if (back) "a" else "e")
    }

    /** Dative "-(y)e/-(y)a": "on" -> "a", "iki" -> "ye". */
    fun dative(word: String): String {
        val back = lastVowel(word)?.let { it !in FRONT_VOWELS } ?: false
        val endsWithVowel = word.lastOrNull()?.lowercaseChar()?.let { it in VOWELS } ?: false
        return (if (endsWithVowel) "y" else "") + (if (back) "a" else "e")
    }

    /** Genitive "-(n)ın/in/un/ün": "Mehmet" -> "in", "Ayşe" -> "nin". */
    fun genitive(word: String): String {
        val vowel = lastVowel(word) ?: 'e'
        val harmony = when (vowel) {
            'a', 'ı' -> "ın"
            'e', 'i' -> "in"
            'o', 'u' -> "un"
            else -> "ün"
        }
        val endsWithVowel = word.lastOrNull()?.lowercaseChar()?.let { it in VOWELS } ?: false
        return (if (endsWithVowel) "n" else "") + harmony
    }

    /** Word for the last spoken part of a whole number: 136 -> "altı", 40 -> "kırk", 100 -> "yüz". */
    fun numberWord(number: Long): String {
        val n = kotlin.math.abs(number)
        val tens = mapOf(6 to "altmış", 7 to "yetmiş", 8 to "seksen", 9 to "doksan") + tensWords
        return when {
            n == 0L -> digitWords[0]
            n % 10 != 0L -> digitWords[(n % 10).toInt()]
            n % 100 != 0L -> tens[((n / 10) % 10).toInt()] ?: digitWords[0]
            n % 1000 != 0L -> "yüz"
            n % 1_000_000 != 0L -> "bin"
            else -> "milyon"
        }
    }

    /** Third-person possessive "-(s)ı/i/u/ü": "iki" -> "si", "üç" -> "ü". */
    fun possessive(word: String): String {
        val harmony = when (lastVowel(word) ?: 'e') {
            'a', 'ı' -> "ı"
            'e', 'i' -> "i"
            'o', 'u' -> "u"
            else -> "ü"
        }
        val endsWithVowel = word.lastOrNull()?.lowercaseChar()?.let { it in VOWELS } ?: false
        return (if (endsWithVowel) "s" else "") + harmony
    }

    /** "2" -> "2'si", "136" -> "136'sı" in Turkish; the plain number in other languages. */
    fun countPossessive(count: Int, locale: Locale = Locale.getDefault()): String =
        if (isTurkish(locale)) "$count'${possessive(numberWord(count.toLong()))}" else count.toString()

    fun timeLocative(formatted: String, time: LocalTime): String = "$formatted'${locative(spokenEnding(time))}"

    fun timeDative(formatted: String, time: LocalTime): String = "$formatted'${dative(spokenEnding(time))}"

    fun nameGenitive(name: String): String = "$name'${genitive(name)}"

    fun isTurkish(locale: Locale = Locale.getDefault()): Boolean = locale.language == "tr"

    private fun lastVowel(word: String): Char? =
        word.lowercase(Locale.forLanguageTag("tr")).lastOrNull { it in VOWELS }
}

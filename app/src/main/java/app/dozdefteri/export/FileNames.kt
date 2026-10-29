package app.dozdefteri.export

import java.text.Normalizer
import java.time.LocalDate

/** Builds file names that are safe on every storage provider. Pure Kotlin. */
object FileNames {
    private val turkishMap = mapOf(
        'ç' to 'c', 'Ç' to 'C', 'ğ' to 'g', 'Ğ' to 'G', 'ı' to 'i', 'İ' to 'I',
        'ö' to 'o', 'Ö' to 'O', 'ş' to 's', 'Ş' to 'S', 'ü' to 'u', 'Ü' to 'U'
    )

    /** "Ayşe Gül" -> "Ayse_Gul". Falls back to "profile" when nothing usable remains. */
    fun asciiSlug(text: String): String {
        val mapped = text.map { turkishMap[it] ?: it }.joinToString("")
        val stripped = Normalizer.normalize(mapped, Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "")
        val slug = stripped.replace(Regex("[^A-Za-z0-9]+"), "_").trim('_')
        return slug.ifEmpty { "profile" }
    }

    fun csv(profileName: String, start: LocalDate, end: LocalDate): String =
        "dozdefteri_${asciiSlug(profileName)}_${start}_$end.csv"

    fun pdf(profileName: String, start: LocalDate, end: LocalDate): String =
        "dozdefteri_${asciiSlug(profileName)}_${start}_$end.pdf"

    fun backup(date: LocalDate): String = "dozdefteri_yedek_$date.json"
}

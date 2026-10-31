package app.dozdefteri.core.format

import java.util.Locale

/** Builds the dose texts shown on cards and notifications. Pure Kotlin. */
object DoseText {
    /** "500 mg", or null when no strength was entered. */
    fun strength(amount: Double?, unit: String?, locale: Locale = Locale.getDefault()): String? =
        if (amount == null || unit.isNullOrBlank()) null else "${Formats.amount(amount, locale)} $unit"

    /** "500 mg · 1 tablet · tok karnına" — strength, amount per dose and note, skipping missing parts. */
    fun line(
        strength: String?,
        doseAmount: Double,
        doseUnit: String,
        note: String?,
        locale: Locale = Locale.getDefault()
    ): String = listOfNotNull(strength, Formats.dose(doseAmount, doseUnit, locale), note?.trim()?.ifEmpty { null })
        .joinToString(SEPARATOR)

    /** "Metformin 500 mg" — notification title. */
    fun title(name: String, strength: String?): String = if (strength == null) name else "$name $strength"

    const val SEPARATOR = " · "
}

package com.margelo.nitro.nitrodatepicker

import java.util.Locale

/**
 * Computes the left-to-right display order of the spinner wheels for a given locale.
 *
 * The order is derived from the locale's own datetime pattern (so `en_US` → "year month date",
 * `en_GB` → "date month year", `zh` → "year month date", etc.), with two fixed rules henninghall
 * follows:
 *   1. The [WheelType.DAY] wheel (the human "Mon, Jan 5" wheel used in `datetime` mode) always
 *      renders first, regardless of locale.
 *   2. The [WheelType.AM_PM] wheel renders last if the locale pattern doesn't already place it.
 *
 * Direct port of `DerivedData.getOrderedWheels` + `Utils.patternCharToWheelType`.
 */
object WheelOrder {

    // Precompiled patterns used to strip quoted literals from the ICU datetime pattern. These run
    // once per show(), so the saving is small, but compiling them once is strictly better than
    // re-compiling on every call.
    private val parensQuoted = Regex("\\('(.+?)'\\)")
    private val quotedLiteral = Regex("'.+?'")
    private val placeholderOut = Regex("\\$\\{(.+?)\\}")

    /**
     * @param locale the active locale (drives ordering)
     * @param visibleWheels the set of wheels that are visible for the current mode
     * @return the visible wheels in left-to-right display order
     */
    fun orderedVisible(locale: Locale, visibleWheels: Set<WheelType>): List<WheelType> {
        val ordered = getOrderedWheels(locale)
        return ordered.filter { it in visibleWheels }
    }

    private fun getOrderedWheels(locale: Locale): List<WheelType> {
        // Strip quoted literals from the pattern so quoted letters (e.g. "de" in "d 'de' MMM")
        // don't get mistaken for pattern chars. We temporarily replace their contents, then drop
        // them — same trick henninghall uses.
        val pattern = LocaleSupport.getDateTimePattern(locale)
            .replace(parensQuoted, "\${$1}")
            .replace(quotedLiteral, "")
            .replace(placeholderOut, "('$1')")

        val unordered = enumValues<WheelType>().toMutableList()
        val ordered = mutableListOf<WheelType>()

        // Rule 1: day wheel is always first.
        unordered.remove(WheelType.DAY)
        ordered.add(WheelType.DAY)

        for (c in pattern) {
            val type = patternCharToWheelType(c) ?: continue
            if (unordered.remove(type)) {
                ordered.add(type)
            }
        }

        // Rule 2: AM/PM last if the locale didn't already place it.
        if (unordered.remove(WheelType.AM_PM)) {
            ordered.add(WheelType.AM_PM)
        }

        return ordered
    }

    /** Map an ICU `SimpleDateFormat` pattern char to a [WheelType], or null if it's irrelevant. */
    private fun patternCharToWheelType(c: Char): WheelType? = when (c) {
        'y' -> WheelType.YEAR
        'M', 'L' -> WheelType.MONTH
        'd' -> WheelType.DATE
        'h', 'H', 'K', 'k' -> WheelType.HOUR
        'm' -> WheelType.MINUTE
        'a' -> WheelType.AM_PM
        else -> null
    }
}

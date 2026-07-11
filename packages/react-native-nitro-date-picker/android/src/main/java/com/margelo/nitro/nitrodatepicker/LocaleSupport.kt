package com.margelo.nitro.nitrodatepicker

import android.text.format.DateFormat
import java.text.DateFormat as JDateFormat
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Locale helpers centralising the two fiddly things henninghall needs:
 *  - turning a JS locale tag (`en_US`, `zh-Hans-CN`, `pt-BR`) into a `java.util.Locale`
 *  - deciding whether a locale renders time in 12-hour (AM/PM) form
 *
 * Standalone (no apache-commons-lang dependency — that lib was used by the original only for
 * `LocaleUtils.toLocale`, which the JDK handles well enough for the tags this library accepts).
 */
object LocaleSupport {

    /**
     * Parse a locale tag from JS. Accepts both `_` (Java/POSIX) and `-` (BCP-47) separators, and
     * handles the `zh_Hans_CN` / `zh-Hans-CN` script form via JDK `Locale.forLanguageTag`. Falls
     * back to the device locale on any failure.
     */
    fun parseLocaleTag(tag: String?): Locale {
        if (tag.isNullOrBlank()) return Locale.getDefault()
        val parsed = Locale.forLanguageTag(tag.replace('_', '-'))
        // forLanguageTag never throws, but returns the root locale for garbage input.
        return if (parsed.language.isNullOrBlank()) Locale.getDefault() else parsed
    }

    /** True when [locale]'s time pattern contains an AM/PM marker (`a`). */
    fun localeUsesAmPm(locale: Locale): Boolean {
        val format = JDateFormat.getTimeInstance(JDateFormat.FULL, locale)
        return format is SimpleDateFormat && format.toPattern().contains("a")
    }

    /** True when the *device* setting (status bar "Use 24-hour format") selects 12-hour. */
    fun deviceUsesAmPm(): Boolean = !DateFormat.is24HourFormat(com.margelo.nitro.NitroModules.applicationContext)

    /**
     * The combined datetime pattern for [locale], used to derive wheel order. Matches henninghall's
     * `LocaleUtils.getDateTimePattern` (FULL/FULL instance, commas stripped).
     */
    fun getDateTimePattern(locale: Locale): String {
        val format = JDateFormat.getDateTimeInstance(JDateFormat.FULL, JDateFormat.FULL, locale)
        return (format as SimpleDateFormat).toLocalizedPattern().replace(",", "")
    }
}

package com.margelo.nitro.nitrodatepicker

import kotlin.math.abs
import java.util.Calendar
import java.util.Locale
import java.util.SimpleTimeZone
import java.util.TimeZone

/**
 * Read-mostly view over a [DatePickerConfig] that the wheels consult during rendering and parsing.
 *
 * Holds the derived `Calendar` representations of the config's epoch-ms values, and the
 * locale/timezone resolution logic. A new [DatePickerState] is built per `show()` call; wheels are
 * rebuilt together with it, so we don't need to support live mutation here.
 */
class DatePickerState(config: DatePickerConfig) {

    private val mode: DatePickerMode = config.mode
    private val locale: Locale = LocaleSupport.parseLocaleTag(config.locale)
    private val timeZone: TimeZone = resolveTimeZone(config.timeZoneOffsetInMinutes)
    private val minuteInterval: Int = config.minuteInterval.toInt().coerceAtLeast(1)
    private val is24HourSource: HourSource? = config.is24HourSource

    /** Initial date the picker opens at. */
    private val pickerDate: Calendar = toCalendar(config.date)

    /** Optional bounds. `null` means unbounded on that side. */
    private val minimumDate: Calendar? = config.minimumDate?.let { toCalendar(it) }
    private val maximumDate: Calendar? = config.maximumDate?.let { toCalendar(it) }

    fun getMode(): DatePickerMode = mode
    fun getLocale(): Locale = locale
    fun getTimeZone(): TimeZone = timeZone
    fun getMinuteInterval(): Int = minuteInterval
    fun getPickerDate(): Calendar = pickerDate
    fun getMinimumDate(): Calendar? = minimumDate
    fun getMaximumDate(): Calendar? = maximumDate

    /** The set of wheels that should be visible for the current mode + AM/PM decision. */
    fun getVisibleWheels(): Set<WheelType> {
        val visible = LinkedHashSet<WheelType>()
        when (mode) {
            DatePickerMode.DATETIME -> {
                visible.add(WheelType.DAY)
                visible.add(WheelType.HOUR)
                visible.add(WheelType.MINUTE)
            }
            DatePickerMode.TIME -> {
                visible.add(WheelType.HOUR)
                visible.add(WheelType.MINUTE)
            }
            DatePickerMode.DATE -> {
                visible.add(WheelType.YEAR)
                visible.add(WheelType.MONTH)
                visible.add(WheelType.DATE)
            }
        }
        if (mode != DatePickerMode.DATE && derivedUsesAmPm()) {
            visible.add(WheelType.AM_PM)
        }
        return visible
    }

    /**
     * Whether the UI should show an AM/PM wheel. `is24HourSource=locale` defers to the locale's
     * own pattern; `device` (or unset) defers to the device's "Use 24-hour format" setting —
     * matching henninghall's `DerivedData.usesAmPm()`.
     */
    fun derivedUsesAmPm(): Boolean {
        return when (is24HourSource) {
            HourSource.LOCALE -> LocaleSupport.localeUsesAmPm(locale)
            HourSource.DEVICE -> LocaleSupport.deviceUsesAmPm()
            null -> LocaleSupport.deviceUsesAmPm()
        }
    }

    /** Visible wheels ordered for the current locale (left → right). */
    fun getOrderedVisibleWheels(): List<WheelType> =
        WheelOrder.orderedVisible(locale, getVisibleWheels())

    private fun resolveTimeZone(offsetInMinutes: Double?): TimeZone {
        // null → device timezone; otherwise a fixed GMT offset expressed in minutes east of UTC.
        val offset = offsetInMinutes?.toInt() ?: return TimeZone.getDefault()
        return SimpleTimeZone(offset * 60_000, "GMT${if (offset >= 0) "+" else ""}${offset / 60}:${abs(offset) % 60}")
    }

    private fun toCalendar(epochMs: Double): Calendar {
        val cal = Calendar.getInstance(timeZone)
        cal.timeInMillis = epochMs.toLong()
        return cal
    }
}

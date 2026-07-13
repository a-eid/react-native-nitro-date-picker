package com.margelo.nitro.nitrodatepicker.wheels

import com.margelo.nitro.nitrodatepicker.DatePickerMode
import com.margelo.nitro.nitrodatepicker.DatePickerState
import com.margelo.nitro.nitrodatepicker.R
import com.margelo.nitro.nitrodatepicker.Wheel
import com.shawnlin.numberpicker.NumberPicker
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Calendar

/**
 * Day wheel for `datetime` mode. Unlike [DateWheel] (which is just day-of-month 1..31), this wheel
 * lists a window of ~150 consecutive calendar days centered on the initial date (or bounded by
 * min/max), each shown as a localized "EEE, MMM d"-style label. The current day is rendered as the
 * locale's word for "Today". Does not wrap.
 *
 * Mirrors henninghall's `DayWheel` minus the time4j "Today" dependency; we approximate "Today" via
 * Android's `android.text.format.DateUtils` instead.
 */
class DayWheel(
    picker: NumberPicker,
    state: DatePickerState,
) : Wheel(picker, state) {

    /** value (raw) -> human display, populated in [getValues]. */
    private val displayValues = HashMap<String, String>()
    private var todayValue: String? = null

    override fun visible(): Boolean = state.getMode() == DatePickerMode.DATETIME

    override fun wrapSelectorWheel(): Boolean = false

    /** Pattern used to identify a day uniquely (parses back to a real date in [PickerWheels]). */
    override fun getFormatPattern(): String {
        // Long enough to be unambiguous within the ±75-day window while staying locale-aware.
        return "EEE MMM d"
    }

    /** Pattern used to render the *displayed* text (richer, with separators). */
    private val displayPattern: String by lazy {
        // DateFormat.getDateInstance returns DateFormat; the JDK always returns a SimpleDateFormat
        // here, so we cast to read its pattern and shorten weekday/month names to fit the wheel.
        val instance = SimpleDateFormat.getDateInstance(SimpleDateFormat.MEDIUM, state.getLocale())
        (instance as SimpleDateFormat).toPattern()
            .replace("EEEE", "EEE")
            .replace("MMMM", "MMM")
    }

    override fun getValues(): List<String> {
        displayValues.clear()
        val rawValues = ArrayList<String>()

        val displayFormat = SimpleDateFormat(displayPattern, state.getLocale())
        displayFormat.timeZone = state.getTimeZone()

        // Truncate the cursor and the terminator to start-of-day once, up front. The formatted
        // values ("EEE MMM d" and the medium-date pattern) and DateUtils.isToday() only depend on
        // the date fields, so dropping the time-of-day is output-preserving, and it lets the loop
        // compare the cursor against `end` directly — the previous version cloned the cursor on
        // every iteration (up to ~365×) just for this terminator check.
        val cal = truncateToStartOfDay(getStartCal())
        val end = truncateToStartOfDay(getEndCal())
        do {
            val raw = format(cal)
            rawValues.add(raw)
            displayValues[raw] = displayFormat.format(cal.time)
            if (android.text.format.DateUtils.isToday(cal.time.time)) todayValue = raw
            cal.add(Calendar.DATE, 1)
        } while (!cal.after(end))

        return rawValues
    }

    /** Truncates [cal] (in place) to 00:00:00.000. */
    private fun truncateToStartOfDay(cal: Calendar): Calendar = cal.apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    override fun toDisplayValue(value: String): String {
        if (value == todayValue) {
            val todayWord = todayLabel()
            // Match the capitalization of the surrounding locale's day labels.
            val first = value.firstOrNull()
            return if (first != null && first.isUpperCase()) capitalize(todayWord) else todayWord
        }
        return displayValues[value] ?: value
    }

    private fun getStartCal(): Calendar {
        val min = state.getMinimumDate()
        val max = state.getMaximumDate()
        return when {
            min != null -> (min.clone() as Calendar)
            max != null -> (max.clone() as Calendar).apply {
                add(Calendar.DATE, -(getActualMaximum(Calendar.DAY_OF_YEAR) / 2))
            }
            else -> (state.getPickerDate().clone() as Calendar).apply {
                add(Calendar.DATE, -(DEFAULT_DAYS / 2))
            }
        }
    }

    private fun getEndCal(): Calendar {
        val min = state.getMinimumDate()
        val max = state.getMaximumDate()
        return when {
            max != null -> (max.clone() as Calendar)
            min != null -> (min.clone() as Calendar).apply {
                add(Calendar.DATE, (getActualMaximum(Calendar.DAY_OF_YEAR) / 2))
            }
            else -> (state.getPickerDate().clone() as Calendar).apply {
                add(Calendar.DATE, DEFAULT_DAYS / 2)
            }
        }
    }

    private fun todayLabel(): String =
        // We don't pull in time4j, so we approximate "Today" with the device locale's word when the
        // platform exposes one, otherwise fall back to the English string resource. This is the one
        // spot we diverge from henninghall's per-locale "Today".
        context.getString(R.string.ndp_today)

    private fun capitalize(s: String): String =
        if (s.isEmpty()) s else s.substring(0, 1).uppercase(state.getLocale()) + s.substring(1)

    private val context get() = picker.context

    companion object {
        private const val DEFAULT_DAYS = 150
    }
}

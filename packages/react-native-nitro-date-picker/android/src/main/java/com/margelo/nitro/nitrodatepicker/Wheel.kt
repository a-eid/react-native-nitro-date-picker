package com.margelo.nitro.nitrodatepicker

import android.graphics.Color
import android.view.View
import com.shawnlin.numberpicker.NumberPicker
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Calendar

/**
 * A single spinner wheel bound to one date/time component (year, month, hour, ...).
 *
 * Each concrete subclass declares a [SimpleDateFormat] pattern ([getFormatPattern]) and a list of
 * raw values ([getValues]) produced by formatting a [Calendar] with that pattern. The wheel's
 * index space is `0..values.size-1`. Going from a [Calendar] to a wheel position (and back) is the
 * core round-trip that [PickerWheels] orchestrates.
 *
 * Mirrors `com.henninghall.date_picker.wheels.Wheel`, adapted to the ShawnLin `NumberPicker` API.
 */
abstract class Wheel(
    val picker: NumberPicker,
    protected val state: DatePickerState,
) {
    /** Raw (un-displayed) values, one per wheel index. Populated by [init]. */
    private val values: ArrayList<String> = ArrayList()

    /**
     * `value → index` over [values], built in [init]. Turns [getIndexOfDate] from an O(n) linear
     * scan into an O(1) lookup. This matters most for [wheels.DayWheel] (n up to ~365) and for
     * [wheels.YearWheel] (n up to ~200): `setValue` runs for every wheel on every
     * `syncWheelsToDate`, including the up-to-10-iteration "nearest valid past date" walk.
     *
     * Duplicate values would collide; concrete wheels generate unique formatted strings per index
     * (a date component formatted with a fixed pattern), so this is safe in practice.
     */
    private val valueToIndex: HashMap<String, Int> = HashMap()

    /** Count of values currently in the wheel's index space. */
    val valuesSize: Int get() = values.size

    /** Backing formatter; rebuilt on locale change in [refresh]. */
    private var formatter: SimpleDateFormat = SimpleDateFormat(getFormatPattern(), state.getLocale())

    /** Last value set via [setValue]; used while the wheel is hidden (see [getValue]). */
    private var userSetValue: Calendar? = null

    init {
        // Wheel text alignment and wrapping are fixed properties of a component type, so set them
        // once at construction.
        picker.wrapSelectorWheel = wrapSelectorWheel()
    }

    /* ------------------------------------------------------------------ */
    /* Subclass contract                                                  */
    /* ------------------------------------------------------------------ */

    /** Whether this wheel should be shown for the current [DatePickerState.getMode]. */
    abstract fun visible(): Boolean

    /** True if the wheel should wrap around at the ends (e.g. minutes), false to stop (years). */
    abstract fun wrapSelectorWheel(): Boolean

    /** The `SimpleDateFormat` pattern used both to *generate* and to *parse* this wheel's value. */
    abstract fun getFormatPattern(): String

    /** Builds the list of raw values for this wheel, in index order. */
    protected abstract fun getValues(): List<String>

    /**
     * Optional per-value display override (e.g. DayWheel shows "Today" for the current day).
     * Default returns the raw value unchanged.
     */
    open fun toDisplayValue(value: String): String = value

    /* ------------------------------------------------------------------ */
    /* Lifecycle                                                          */
    /* ------------------------------------------------------------------ */

    /** (Re)populate the wheel's value space and displayed values. Idempotent. */
    fun init() {
        values.clear()
        values.addAll(getValues())

        valueToIndex.clear()
        valueToIndex.ensureCapacity(values.size)
        for (i in values.indices) valueToIndex[values[i]] = i

        // NumberPicker requires minValue/maxValue to be set before displayed values, and maxValue
        // must equal values.size - 1 after the call.
        picker.minValue = 0
        picker.maxValue = 0
        picker.displayedValues = values.map { toDisplayValue(it) }.toTypedArray()
        picker.maxValue = values.size - 1
    }

    /** Show or hide this wheel based on [visible]. */
    fun updateVisibility() {
        picker.visibility = if (visible()) View.VISIBLE else View.GONE
    }

    /* ------------------------------------------------------------------ */
    /* Value round-trip                                                   */
    /* ------------------------------------------------------------------ */

    /** The raw string value at the wheel's current index. */
    fun getValue(): String {
        if (!visible()) {
            // Hidden wheels have no index; report the last value the user explicitly set.
            userSetValue?.let { return formatter.format(it.time) }
            return formatter.format(state.getPickerDate().time)
        }
        return getValueAtIndex(picker.value)
    }

    /**
     * The raw value [subtractIndex] positions before the current one, wrapping within the value
     * space. Used by [PickerWheels.nearestValidDateInPast] to walk the day wheel backwards while
     * leaving the other wheels at their current position. Matches henninghall's `getPastValue`.
     */
    fun getPastValue(subtractIndex: Int): String {
        if (!visible()) {
            userSetValue?.let {
                val shifted = (it.clone() as java.util.Calendar)
                shifted.add(java.util.Calendar.DATE, -subtractIndex)
                return formatter.format(shifted.time)
            }
            return getValue()
        }
        val size = valuesSize
        if (size == 0) return getValue()
        val idx = ((picker.value - subtractIndex) % size + size) % size
        return getValueAtIndex(idx)
    }

    /** Raw value at [index], bounds-checked. */
    fun getValueAtIndex(index: Int): String = values[index]

    fun getIndex(): Int = picker.value

    /**
     * Move the wheel to the position matching [date]. Pass [animate] = true to smooth-scroll
     * (e.g. when clamping after a user edit); false to jump (during initialisation).
     */
    fun setValue(date: Calendar, animate: Boolean = false) {
        userSetValue = date
        formatter.timeZone = state.getTimeZone()
        val index = getIndexOfDate(date)
        if (index < 0) return
        if (animate) picker.smoothScrollToPosition(index) else picker.value = index
    }

    private fun getIndexOfDate(date: Calendar): Int {
        formatter.timeZone = state.getTimeZone()
        return valueToIndex[formatter.format(date.time)] ?: -1
    }

    /* ------------------------------------------------------------------ */
    /* Styling                                                            */
    /* ------------------------------------------------------------------ */

    /** Apply an arbitrary hex text color to every wheel item. */
    fun setTextColor(hex: String?) {
        val color = parseColor(hex) ?: return
        picker.setTextColor(color)
        // ShawnLin exposes setSelectedTextColor separately; keep them aligned so the focused row
        // does not flash a different color.
        picker.setSelectedTextColor(color)
    }

    /** Apply an arbitrary hex divider color. */
    fun setDividerColor(hex: String?) {
        val color = parseColor(hex) ?: return
        picker.setDividerColor(color)
    }

    /** Apply a selected-row text color (defaults to the regular text color when not set). */
    fun setSelectedTextColor(hex: String?) {
        val color = parseColor(hex) ?: return
        picker.setSelectedTextColor(color)
    }

    /** Text size in sp for non-selected rows. */
    fun setTextSize(sp: Float?) {
        if (sp != null && sp > 0) picker.setTextSize(sp)
    }

    /** Text size in sp for the selected row. */
    fun setSelectedTextSize(sp: Float?) {
        if (sp != null && sp > 0) picker.setSelectedTextSize(sp)
    }

    /** Thickness of the divider lines, in dp. */
    fun setDividerThickness(dp: Float?) {
        if (dp != null && dp >= 0) picker.setDividerThickness(dpToPx(dp))
    }

    /** Vertical distance between the two divider lines, in dp. */
    fun setDividerDistance(dp: Float?) {
        if (dp != null && dp >= 0) picker.setDividerDistance(dpToPx(dp))
    }

    /** Extra vertical gap between the selected row and its neighbours, in dp. */
    fun setItemSpacing(dp: Float?) {
        if (dp != null && dp >= 0) picker.setItemSpacing(dpToPx(dp))
    }

    /** Number of rows shown on the wheel (clamped to a minimum of 3 by the picker). */
    fun setWheelItemCount(count: Int?) {
        if (count != null && count >= 3) picker.setWheelItemCount(count)
    }

    protected fun parseColor(hex: String?): Int? {
        if (hex.isNullOrBlank()) return null
        return try {
            Color.parseColor(hex)
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    protected fun dpToPx(dp: Float): Int =
        (dp * picker.resources.displayMetrics.density).toInt()

    /** Format a calendar using this wheel's pattern + locale (for subclass value generation). */
    protected fun format(cal: Calendar): String {
        formatter.timeZone = state.getTimeZone()
        return formatter.format(cal.time)
    }
}

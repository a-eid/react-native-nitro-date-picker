package com.margelo.nitro.nitrodatepicker

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.margelo.nitro.nitrodatepicker.wheels.AmPmWheel
import com.margelo.nitro.nitrodatepicker.wheels.DateWheel
import com.margelo.nitro.nitrodatepicker.wheels.DayWheel
import com.margelo.nitro.nitrodatepicker.wheels.HourWheel
import com.margelo.nitro.nitrodatepicker.wheels.MinutesWheel
import com.margelo.nitro.nitrodatepicker.wheels.MonthWheel
import com.margelo.nitro.nitrodatepicker.wheels.YearWheel
import com.shawnlin.numberpicker.NumberPicker
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar

/**
 * Owns the seven spinner wheels, their on-screen layout, and the round-trip between the wheels'
 * positions and a single `Calendar` value.
 *
 * On every wheel change we:
 *   1. Concatenate each visible wheel's raw value (in display order) into a datetime string.
 *   2. Parse it with a master pattern built from the same per-wheel patterns, in the configured
 *      timezone, `setLenient(false)` to reject non-existent dates (Apr 31, etc.).
 *   3. If invalid, animate back to the nearest valid past date (henninghall behaviour).
 *   4. If outside [DatePickerState.getMinimumDate]/[getMaximumDate], animate to the boundary.
 *   5. Otherwise fire [onDateChange] with the resolved timestamp.
 *
 * This is the union of henninghall's `Wheels`, `UIManager` and `WheelChangeListenerImpl`.
 */
class PickerWheels(
    private val context: Context,
    private val state: DatePickerState,
) {
    /** Notified whenever the wheels settle on a new, valid, in-range date. */
    var onDateChange: ((Calendar) -> Unit)? = null

    private lateinit var root: LinearLayout
    private lateinit var pickerWrapper: LinearLayout

    private lateinit var yearWheel: YearWheel
    private lateinit var monthWheel: MonthWheel
    private lateinit var dateWheel: DateWheel
    private lateinit var dayWheel: DayWheel
    private lateinit var hourWheel: HourWheel
    private lateinit var minutesWheel: MinutesWheel
    private lateinit var amPmWheel: AmPmWheel

    private val byType: Map<WheelType, Wheel> get() = mapOf(
        WheelType.YEAR to yearWheel,
        WheelType.MONTH to monthWheel,
        WheelType.DATE to dateWheel,
        WheelType.DAY to dayWheel,
        WheelType.HOUR to hourWheel,
        WheelType.MINUTE to minutesWheel,
        WheelType.AM_PM to amPmWheel,
    )

    /** Tracks whether any wheel is mid-scroll, to avoid emitting half-spun dates. */
    private var spinning = false

    /**
     * Builds the picker view tree. Returns the root [View] to attach into the dialog.
     * Idempotent for a given instance; call once per `show()`.
     */
    fun build(): View {
        val inflater = LayoutInflater.from(context)
        // Attach to a root so the XML's own layout params are preserved; we hand the inflated view
        // to the dialog later via wrapInView().
        root = inflater.inflate(R.layout.spinner_picker, null, false) as LinearLayout
        pickerWrapper = root

        fun find(id: Int): NumberPicker = root.findViewById(id)

        yearWheel = YearWheel(find(R.id.year), state)
        monthWheel = MonthWheel(find(R.id.month), state)
        dateWheel = DateWheel(find(R.id.date), state)
        dayWheel = DayWheel(find(R.id.day), state)
        hourWheel = HourWheel(find(R.id.hour), state)
        minutesWheel = MinutesWheel(find(R.id.minutes), state)
        amPmWheel = AmPmWheel(find(R.id.ampm), state)

        byType.values.forEach { wheel ->
            wheel.init()
            wheel.updateVisibility()
        }

        reorderWheels()
        wireChangeListeners()
        syncWheelsToDate(state.getPickerDate())

        return root
    }

    /** Apply a hex text color to every wheel. No-op if [hex] is null/blank/invalid. */
    fun applyTextColor(hex: String?) {
        byType.values.forEach { it.setTextColor(hex) }
    }

    /** Apply a hex divider color to every wheel. No-op if [hex] is null/blank/invalid. */
    fun applyDividerColor(hex: String?) {
        byType.values.forEach { it.setDividerColor(hex) }
    }

    /**
     * Apply a full [PickerStyle] to every wheel. Each field is a no-op when null, so callers pass
     * the resolved value (already merged with theme defaults + legacy props upstream).
     */
    fun applyStyle(style: PickerStyle) {
        byType.values.forEach { wheel ->
            wheel.setSelectedTextColor(style.selectedTextColor ?: style.textColor)
            wheel.setTextSize(style.textSize?.toFloat())
            wheel.setSelectedTextSize(style.selectedTextSize?.toFloat())
            wheel.setDividerThickness(style.dividerThickness?.toFloat())
            wheel.setDividerDistance(style.dividerDistance?.toFloat())
            wheel.setItemSpacing(style.itemSpacing?.toFloat())
            wheel.setWheelItemCount(style.wheelItemCount?.toInt())
        }
    }

    /** Apply a fixed height (in dp) to the wheel layout root. */
    fun applyHeight(dp: Float?) {
        if (dp == null || dp <= 0) return
        val px = (dp * root.resources.displayMetrics.density).toInt()
        // The layout is inflated without a parent, so layoutParams may be null — create one if so.
        val params = root.layoutParams ?: ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            px,
        )
        params.height = px
        root.layoutParams = params
    }

    /** The date the wheels currently express. Safe to call after [build]. */
    fun getCurrentDate(): Calendar {
        val parsed = parseWheels(lenient = true)
        if (parsed != null && inRange(parsed)) return parsed
        return state.getPickerDate()
    }

    // ------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------

    private fun reorderWheels() {
        // Detach every wheel, then re-add the visible ones in locale order so the LinearLayout lays
        // them out left-to-right correctly. (Invisible wheels stay detached.)
        (0 until pickerWrapper.childCount).toList().forEach { pickerWrapper.getChildAt(0)?.let { pickerWrapper.removeView(it) } }
        state.getOrderedVisibleWheels().forEach { type ->
            byType[type]?.picker?.let { picker ->
                val parent = picker.parent as? LinearLayout
                parent?.removeView(picker)
                pickerWrapper.addView(picker)
            }
        }
    }

    private fun wireChangeListeners() {
        byType.values.forEach { wheel ->
            wheel.picker.setOnValueChangedListener { picker, oldVal, newVal ->
                // AM/PM auto-flip: when the hour wheel crosses 11↔12 in a 12-hour locale, advance
                // the AM/PM wheel so the displayed time tracks the user's intent. The original
                // uses a separate "in-scrolling" listener for this; we fold it into the single
                // listener the ShawnLin picker exposes.
                if (state.derivedUsesAmPm() && wheel === hourWheel) {
                    val oldValue = hourWheel.getValueAtIndex(oldVal)
                    val newValue = hourWheel.getValueAtIndex(newVal)
                    val crossing =
                        ("12" == oldValue && "11" == newValue) || ("11" == oldValue && "12" == newValue)
                    if (crossing) {
                        amPmWheel.picker.value = (amPmWheel.getIndex() + 1) % 2
                    }
                }
                // NumberPicker fires onValueChange during scrolling too; debounce via scroll state.
                handleWheelChange()
            }
            wheel.picker.setOnScrollListener { _, newState ->
                val wasSpinning = spinning
                spinning = newState != NumberPicker.OnScrollListener.SCROLL_STATE_IDLE
                if (wasSpinning && !spinning) {
                    // Wheel just came to rest — emit the settled value.
                    handleWheelChange()
                }
            }
        }
    }

    private fun handleWheelChange() {
        if (spinning) return

        // Reject non-existent dates (e.g. Apr 31) by snapping to the nearest valid past date.
        if (parseWheels(lenient = false) == null) {
            nearestValidDateInPast()?.let { syncWheelsToDate(it, animate = true) }
            return
        }

        val selected = parseWheels(lenient = true) ?: return

        val min = state.getMinimumDate()
        if (min != null && selected.before(min)) {
            syncWheelsToDate(min, animate = true)
            return
        }
        val max = state.getMaximumDate()
        if (max != null && selected.after(max)) {
            syncWheelsToDate(max, animate = true)
            return
        }

        onDateChange?.invoke(selected)
    }

    private fun syncWheelsToDate(date: Calendar, animate: Boolean = false) {
        byType.values.forEach { it.setValue(date, animate) }
    }

    // ------------------------------------------------------------------
    // Value round-trip
    // ------------------------------------------------------------------

    /** The combined master pattern covering every visible wheel, in display order. */
    private val masterPattern: String by lazy {
        // AM/PM wheel pattern is " a " (padded); collapse internal whitespace so the joined pattern
        // and the joined value line up token-for-token during parse.
        state.getOrderedVisibleWheels()
            .joinToString(" ") { byType[it]!!.getFormatPattern() }
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun masterFormat(): SimpleDateFormat =
        SimpleDateFormat(masterPattern, state.getLocale()).apply {
            timeZone = state.getTimeZone()
        }

    /**
     * Parse the wheels' current values (with [dayOffset] applied to the DAY wheel, 0 = today) into a
     * `Calendar`. Returns null if the combination is not a real date or doesn't parse. [lenient]
     * controls whether Apr 31 etc. are accepted.
     */
    private fun parseWheels(dayOffset: Int = 0, lenient: Boolean = false): Calendar? {
        val combined = state.getOrderedVisibleWheels().joinToString(" ") { type ->
            // Only the DAY wheel is shifted; other wheels keep their current value. Matches
            // henninghall's getDateTimeString(daysToSubtract).
            val raw = if (type == WheelType.DAY) byType[type]!!.getPastValue(dayOffset) else byType[type]!!.getValue()
            raw
        }.replace(Regex("\\s+"), " ").trim()

        val format = masterFormat().apply { isLenient = lenient }
        val parsed = try {
            format.parse(combined) ?: return null
        } catch (e: ParseException) {
            return null
        }
        return Calendar.getInstance(state.getTimeZone()).apply { time = parsed }
    }

    /** Walk up to 10 days back looking for a real date the wheels can express. */
    private fun nearestValidDateInPast(): Calendar? {
        for (daysBack in 0 until 10) {
            parseWheels(dayOffset = daysBack, lenient = false)?.let { return it }
        }
        return null
    }

    private fun inRange(cal: Calendar): Boolean {
        val min = state.getMinimumDate()
        val max = state.getMaximumDate()
        if (min != null && cal.before(min)) return false
        if (max != null && cal.after(max)) return false
        return true
    }
}

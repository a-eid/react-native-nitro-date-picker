package com.margelo.nitro.nitrodatepicker.wheels

import com.margelo.nitro.nitrodatepicker.DatePickerMode
import com.margelo.nitro.nitrodatepicker.DatePickerState
import com.margelo.nitro.nitrodatepicker.Wheel
import com.shawnlin.numberpicker.NumberPicker
import java.util.ArrayList
import java.util.Calendar

/**
 * Hour wheel, shown in `time` and `datetime` modes. Renders 12 values when the locale/device uses
 * AM/PM (paired with [AmPmWheel]), 24 values otherwise. Wraps.
 */
class HourWheel(
    picker: NumberPicker,
    state: DatePickerState,
) : Wheel(picker, state) {

    override fun visible(): Boolean = state.getMode() != DatePickerMode.DATE

    override fun wrapSelectorWheel(): Boolean = true

    override fun getFormatPattern(): String = if (state.derivedUsesAmPm()) "h" else "HH"

    override fun getValues(): List<String> {
        val values = ArrayList<String>()
        val cal = Calendar.getInstance()
        // Anchor on a year without DST so cal.add(HOUR) behaves consistently.
        cal.set(2000, 0, 1, 0, 0, 0)
        val count = if (state.derivedUsesAmPm()) 12 else 24
        for (i in 0 until count) {
            values.add(format(cal))
            cal.add(Calendar.HOUR_OF_DAY, 1)
        }
        return values
    }
}

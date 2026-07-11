package com.margelo.nitro.nitrodatepicker.wheels

import com.margelo.nitro.nitrodatepicker.DatePickerMode
import com.margelo.nitro.nitrodatepicker.DatePickerState
import com.margelo.nitro.nitrodatepicker.Wheel
import com.shawnlin.numberpicker.NumberPicker
import java.util.ArrayList
import java.util.Calendar

/**
 * Minutes wheel, shown in `time` and `datetime` modes. Steps by [DatePickerState.getMinuteInterval],
 * so for `minuteInterval=15` the wheel offers 00, 15, 30, 45. Wraps.
 */
class MinutesWheel(
    picker: NumberPicker,
    state: DatePickerState,
) : Wheel(picker, state) {

    override fun visible(): Boolean = state.getMode() != DatePickerMode.DATE

    override fun wrapSelectorWheel(): Boolean = true

    override fun getFormatPattern(): String = "mm"

    override fun getValues(): List<String> {
        val values = ArrayList<String>()
        val cal = Calendar.getInstance()
        cal.set(Calendar.MINUTE, 0)
        val step = state.getMinuteInterval().coerceAtLeast(1)
        var m = 0
        while (m < 60) {
            values.add(format(cal))
            cal.add(Calendar.MINUTE, step)
            m += step
        }
        return values
    }
}

package com.margelo.nitro.nitrodatepicker.wheels

import com.margelo.nitro.nitrodatepicker.DatePickerMode
import com.margelo.nitro.nitrodatepicker.DatePickerState
import com.margelo.nitro.nitrodatepicker.Wheel
import com.shawnlin.numberpicker.NumberPicker
import java.util.ArrayList
import java.util.Calendar

/**
 * Day-of-month wheel (1..31), shown only in `date` mode alongside [YearWheel] and [MonthWheel].
 * Wraps. The displayed range is fixed to 1..31; invalid combinations (e.g. April 31) are caught by
 * [PickerWheels] when it parses the combined value and the wheel is snapped back.
 */
class DateWheel(
    picker: NumberPicker,
    state: DatePickerState,
) : Wheel(picker, state) {

    override fun visible(): Boolean = state.getMode() == DatePickerMode.DATE

    override fun wrapSelectorWheel(): Boolean = true

    override fun getFormatPattern(): String = "d"

    override fun getValues(): List<String> {
        val values = ArrayList<String>()
        val cal = Calendar.getInstance()
        cal.set(Calendar.MONTH, 0)
        cal.set(Calendar.DATE, 1)
        for (i in 1..31) {
            values.add(format(cal))
            cal.add(Calendar.DATE, 1)
        }
        return values
    }
}

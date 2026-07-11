package com.margelo.nitro.nitrodatepicker.wheels

import com.margelo.nitro.nitrodatepicker.DatePickerMode
import com.margelo.nitro.nitrodatepicker.DatePickerState
import com.margelo.nitro.nitrodatepicker.Wheel
import com.shawnlin.numberpicker.NumberPicker
import java.util.ArrayList
import java.util.Calendar

/**
 * Month wheel (Jan..Dec in the active locale), shown only in `date` mode. Uses the standalone-month
 * pattern `LLLL` so locales that differentiate (e.g. Russian) show the nominative form. Wraps.
 */
class MonthWheel(
    picker: NumberPicker,
    state: DatePickerState,
) : Wheel(picker, state) {

    override fun visible(): Boolean = state.getMode() == DatePickerMode.DATE

    override fun wrapSelectorWheel(): Boolean = true

    override fun getFormatPattern(): String = "LLLL"

    override fun getValues(): List<String> {
        val values = ArrayList<String>()
        val cal = Calendar.getInstance()
        cal.set(Calendar.MONTH, 0)
        for (i in 0..11) {
            values.add(format(cal))
            cal.add(Calendar.MONTH, 1)
        }
        return values
    }
}

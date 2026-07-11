package com.margelo.nitro.nitrodatepicker.wheels

import com.margelo.nitro.nitrodatepicker.DatePickerMode
import com.margelo.nitro.nitrodatepicker.DatePickerState
import com.margelo.nitro.nitrodatepicker.Wheel
import com.shawnlin.numberpicker.NumberPicker
import java.util.ArrayList
import java.util.Calendar

/**
 * AM/PM wheel, shown only when [DatePickerState.derivedUsesAmPm] is true and the mode includes time.
 * Two values, no wrap.
 */
class AmPmWheel(
    picker: NumberPicker,
    state: DatePickerState,
) : Wheel(picker, state) {

    override fun visible(): Boolean =
        state.derivedUsesAmPm() && state.getMode() != DatePickerMode.DATE

    override fun wrapSelectorWheel(): Boolean = false

    override fun getFormatPattern(): String = if (state.derivedUsesAmPm()) " a " else ""

    override fun getValues(): List<String> {
        val values = ArrayList<String>()
        val cal = Calendar.getInstance()
        cal.set(2000, 0, 1, 0, 0, 0)
        values.add(format(cal))
        cal.add(Calendar.HOUR_OF_DAY, 12)
        values.add(format(cal))
        return values
    }
}

package com.margelo.nitro.nitrodatepicker.wheels

import com.margelo.nitro.nitrodatepicker.DatePickerMode
import com.margelo.nitro.nitrodatepicker.DatePickerState
import com.margelo.nitro.nitrodatepicker.Wheel
import com.shawnlin.numberpicker.NumberPicker
import java.util.ArrayList
import java.util.Calendar

/**
 * Year wheel, shown only in `date` mode. Range is `minimumDate.year .. maximumDate.year`,
 * defaulting to 1900..2100 when unbounded (matches henninghall's `defaultStartYear`/`EndYear`).
 * Does not wrap.
 */
class YearWheel(
    picker: NumberPicker,
    state: DatePickerState,
) : Wheel(picker, state) {

    private val defaultStartYear = 1900
    private val defaultEndYear = 2100

    override fun visible(): Boolean = state.getMode() == DatePickerMode.DATE

    override fun wrapSelectorWheel(): Boolean = false

    override fun getFormatPattern(): String = "y"

    override fun getValues(): List<String> {
        val values = ArrayList<String>()
        val cal = Calendar.getInstance()
        val startYear = getStartYear()
        val endYear = getEndYear()
        cal.set(Calendar.YEAR, startYear)
        for (i in 0..(endYear - startYear)) {
            values.add(format(cal))
            cal.add(Calendar.YEAR, 1)
        }
        return values
    }

    private fun getStartYear(): Int =
        state.getMinimumDate()?.get(Calendar.YEAR) ?: defaultStartYear

    private fun getEndYear(): Int =
        state.getMaximumDate()?.get(Calendar.YEAR) ?: defaultEndYear
}

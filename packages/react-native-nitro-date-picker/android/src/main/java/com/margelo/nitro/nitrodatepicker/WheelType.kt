package com.margelo.nitro.nitrodatepicker

/**
 * The seven date/time components henninghall's UI exposes as individual spinner wheels.
 * Order in this enum is arbitrary; actual display order is locale-derived (see [WheelOrder]).
 */
enum class WheelType {
    YEAR,
    MONTH,
    DATE,
    DAY,
    HOUR,
    MINUTE,
    AM_PM,
}

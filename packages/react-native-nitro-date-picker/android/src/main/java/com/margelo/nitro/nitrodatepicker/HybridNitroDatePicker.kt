package com.margelo.nitro.nitrodatepicker

import android.app.AlertDialog
import android.app.Dialog
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.widget.DatePicker
import android.widget.LinearLayout
import android.widget.TimePicker
import androidx.annotation.Keep
import com.facebook.proguard.annotations.DoNotStrip
import com.margelo.nitro.NitroModules
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Keep
@DoNotStrip
class HybridNitroDatePicker : HybridNitroDatePickerSpec() {

  override var onDateChange: ((timestamp: Double) -> Unit)? = null
  override var onConfirm: ((timestamp: Double) -> Unit)? = null
  override var onCancel: (() -> Unit)? = null

  private var dialog: Dialog? = null
  private var currentDate: Calendar = Calendar.getInstance()

  override fun show(config: DatePickerConfig) {
    currentDate = Calendar.getInstance().apply {
      timeInMillis = config.date.toLong()
    }

    val context = NitroModules.applicationContext
      ?: throw IllegalStateException("ApplicationContext is not available")

    Handler(Looper.getMainLooper()).post {
      val builder = AlertDialog.Builder(context)

      val title = config.title
        ?: when (config.mode) {
          DatePickerMode.TIME -> "Select time"
          else -> "Select date"
        }

      builder.setTitle(title)

      when (config.mode) {
        DatePickerMode.TIME -> setupTimePicker(config, builder, context)
        DatePickerMode.DATE -> setupDatePicker(config, builder, context)
        DatePickerMode.DATETIME -> setupDateTimePicker(config, builder, context)
      }

      builder.setPositiveButton(config.confirmText ?: "Confirm") { _, _ ->
        onConfirm?.invoke(currentDate.timeInMillis.toDouble())
        dialog = null
      }

      builder.setNegativeButton(config.cancelText ?: "Cancel") { _, _ ->
        onCancel?.invoke()
        dialog = null
      }

      val dlg = builder.create()
      dialog = dlg
      dlg.show()
    }
  }

  override fun dismiss() {
    Handler(Looper.getMainLooper()).post {
      dialog?.dismiss()
      dialog = null
    }
  }

  private fun setupDatePicker(
    config: DatePickerConfig,
    builder: AlertDialog.Builder,
    context: android.content.Context
  ) {
    val picker = DatePicker(context)
    picker.init(
      currentDate.get(Calendar.YEAR),
      currentDate.get(Calendar.MONTH),
      currentDate.get(Calendar.DAY_OF_MONTH)
    ) { _, year, monthOfYear, dayOfMonth ->
      currentDate.set(Calendar.YEAR, year)
      currentDate.set(Calendar.MONTH, monthOfYear)
      currentDate.set(Calendar.DAY_OF_MONTH, dayOfMonth)
      onDateChange?.invoke(currentDate.timeInMillis.toDouble())
    }

    if (config.minimumDate != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
      picker.minDate = config.minimumDate.toLong()
    }
    if (config.maximumDate != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
      picker.maxDate = config.maximumDate.toLong()
    }

    builder.setView(picker)
  }

  private fun setupTimePicker(
    config: DatePickerConfig,
    builder: AlertDialog.Builder,
    context: android.content.Context
  ) {
    val picker = TimePicker(context).apply {
      setIs24HourView(shouldUse24Hour(config))
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      picker.hour = currentDate.get(Calendar.HOUR_OF_DAY)
      picker.minute = roundToMinuteInterval(
        currentDate.get(Calendar.MINUTE),
        config.minuteInterval.toInt()
      )
    }

    picker.setOnTimeChangedListener { _, hourOfDay, minute ->
      currentDate.set(Calendar.HOUR_OF_DAY, hourOfDay)
      currentDate.set(
        Calendar.MINUTE,
        roundToMinuteInterval(minute, config.minuteInterval.toInt())
      )
      onDateChange?.invoke(currentDate.timeInMillis.toDouble())
    }

    builder.setView(picker)
  }

  private fun setupDateTimePicker(
    config: DatePickerConfig,
    builder: AlertDialog.Builder,
    context: android.content.Context
  ) {
    val layout = LinearLayout(context).apply {
      orientation = LinearLayout.VERTICAL
      layoutParams = ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      )
    }

    val datePicker = DatePicker(context)
    datePicker.init(
      currentDate.get(Calendar.YEAR),
      currentDate.get(Calendar.MONTH),
      currentDate.get(Calendar.DAY_OF_MONTH)
    ) { _, year, monthOfYear, dayOfMonth ->
      currentDate.set(Calendar.YEAR, year)
      currentDate.set(Calendar.MONTH, monthOfYear)
      currentDate.set(Calendar.DAY_OF_MONTH, dayOfMonth)
      onDateChange?.invoke(currentDate.timeInMillis.toDouble())
    }

    if (config.minimumDate != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
      datePicker.minDate = config.minimumDate.toLong()
    }
    if (config.maximumDate != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
      datePicker.maxDate = config.maximumDate.toLong()
    }

    val timePicker = TimePicker(context).apply {
      setIs24HourView(shouldUse24Hour(config))

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        hour = currentDate.get(Calendar.HOUR_OF_DAY)
        minute = roundToMinuteInterval(
          currentDate.get(Calendar.MINUTE),
          config.minuteInterval.toInt()
        )
      }

      setOnTimeChangedListener { _, hourOfDay, minute ->
        currentDate.set(Calendar.HOUR_OF_DAY, hourOfDay)
        currentDate.set(
          Calendar.MINUTE,
          roundToMinuteInterval(minute, config.minuteInterval.toInt())
        )
        onDateChange?.invoke(currentDate.timeInMillis.toDouble())
      }
    }

    layout.addView(datePicker)
    layout.addView(timePicker)
    builder.setView(layout)
  }

  private fun shouldUse24Hour(config: DatePickerConfig): Boolean {
    return when (config.is24HourSource) {
      HourSource.DEVICE -> android.text.format.DateFormat.is24HourFormat(
        NitroModules.applicationContext
      )
      HourSource.LOCALE -> {
        val locale = if (config.locale != null) {
          Locale.forLanguageTag(config.locale)
        } else {
          Locale.getDefault()
        }
        val sdf = SimpleDateFormat.getTimeInstance(SimpleDateFormat.SHORT, locale) as SimpleDateFormat
        sdf.toPattern().contains("a").not()
      }
      else -> android.text.format.DateFormat.is24HourFormat(
        NitroModules.applicationContext
      )
    }
  }

  private fun roundToMinuteInterval(minute: Int, interval: Int): Int {
    if (interval <= 1) return minute
    return (minute / interval) * interval
  }
}

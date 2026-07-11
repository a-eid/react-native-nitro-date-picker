package com.margelo.nitro.nitrodatepicker

import android.app.Dialog
import androidx.appcompat.app.AlertDialog
import android.content.res.Configuration
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.annotation.Keep
import com.facebook.proguard.annotations.DoNotStrip
import com.margelo.nitro.NitroModules

/**
 * Nitro HybridObject backing `<DatePicker>`.
 *
 * Presents a henninghall-style spinner date/time picker inside an `AlertDialog`. The dialog body is
 * a horizontal row of `NumberPicker` wheels (year/month/day/hour/minute/am-pm as the mode requires)
 * built by [PickerWheels]; the confirm/cancel buttons come from the alert.
 *
 * The JS layer drives this via `show(config)` / `dismiss()` and reads changes back through the
 * `onDateChange`, `onConfirm`, `onCancel` callbacks. See [DatePicker.tsx].
 */
@Keep
@DoNotStrip
class HybridNitroDatePicker : HybridNitroDatePickerSpec() {

    override var onDateChange: ((timestamp: Double) -> Unit)? = null
    override var onConfirm: ((timestamp: Double) -> Unit)? = null
    override var onCancel: (() -> Unit)? = null

    private var dialog: Dialog? = null
    private var wheels: PickerWheels? = null

    override fun show(config: DatePickerConfig) {
        val context = NitroModules.applicationContext
            ?: throw IllegalStateException("ApplicationContext is not available")

        // NitroModules.applicationContext is the application context; AlertDialog needs an Activity.
        // Post to the main looper so we run on the UI thread regardless of the calling thread.
        Handler(Looper.getMainLooper()).post {
            val activity = context.currentActivity
                ?: throw IllegalStateException("No current Activity — cannot show dialog")

            // Tear down any previously-open picker first (the JS side should keep us single-instance,
            // but be defensive — see DatePicker.tsx docs).
            dialog?.dismiss()
            dialog = null
            wheels = null

            val state = DatePickerState(config)
            val picker = PickerWheels(activity, state)
            wheels = picker

            val contentView = picker.build()

            // Resolve colors: style.* ?? theme default. Always concrete so the wheels stay legible.
            val resolvedTextColor = effectiveTextColor(config, activity)
            val resolvedDividerColor = effectiveDividerColor(config, activity)
            picker.applyTextColor(resolvedTextColor)
            picker.applyDividerColor(resolvedDividerColor)

            // Apply the rest of `style` (text sizes, spacing, dividers, height). Each field is a
            // no-op when null, leaving the XML defaults in place.
            config.style?.let {
                picker.applyStyle(it)
                picker.applyHeight(it.height?.toFloat())
            }

            // Forward every in-range change to JS.
            picker.onDateChange = { cal ->
                onDateChange?.invoke(cal.timeInMillis.toDouble())
            }

            val title = config.title ?: defaultTitle(config.mode)
            val builder = AlertDialog.Builder(activity)
                .setTitle(title)
                .setView(wrapInView(contentView, config))

            builder.setPositiveButton(config.confirmText ?: "Confirm") { _, _ ->
                val finalDate = picker.getCurrentDate()
                onConfirm?.invoke(finalDate.timeInMillis.toDouble())
                dialog = null
                wheels = null
            }

            builder.setNegativeButton(config.cancelText ?: "Cancel") { _, _ ->
                onCancel?.invoke()
                dialog = null
                wheels = null
            }

            builder.setOnCancelListener {
                // Back button / outside touch dismisses the dialog — treat as cancel.
                onCancel?.invoke()
                dialog = null
                wheels = null
            }

            val dlg = builder.create()
            dialog = dlg
            // Window background can be set before show; the title view and buttons only exist after
            // show(), so those are tinted afterwards.
            val bg = effectiveBackgroundColor(config, activity)
            dlg.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(bg))
            dlg.show()
            val textColor = parseColorSafe(effectiveTextColor(config, activity))
            // androidx AlertDialog exposes the title via R.id.alertTitle (not android.R.id.title).
            (dlg.findViewById(androidx.appcompat.R.id.alertTitle) as? android.widget.TextView)?.setTextColor(textColor)
            dlg.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(textColor)
            dlg.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(textColor)
        }
    }

    override fun dismiss() {
        Handler(Looper.getMainLooper()).post {
            dialog?.dismiss()
            dialog = null
            wheels = null
        }
    }

    /**
     * The wheel layout is `wrap_content` tall; pad it so the dialog has breathing room and apply the
     * theme background so light/dark themes read correctly.
     */
    private fun wrapInView(content: android.view.View, config: DatePickerConfig): android.view.View {
        val padding = (16 * content.resources.displayMetrics.density).toInt()
        val container = LinearLayout(content.context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding, padding, padding)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
            setBackgroundColor(effectiveBackgroundColor(config, content.context))
        }
        container.addView(content)
        return container
    }

    /** Parse a hex string to a color int, falling back to black on invalid input. */
    private fun parseColorSafe(hex: String): Int =
        try { Color.parseColor(hex) } catch (e: IllegalArgumentException) { Color.BLACK }

    // ------------------------------------------------------------------
    // Theme resolution
    // ------------------------------------------------------------------

    /** True when the device is currently in dark mode (night resources in use). */
    private fun isSystemInDarkMode(context: android.content.Context): Boolean =
        (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES

    /**
     * Resolves `theme` to an effective light/dark choice, with `.auto` (and unset) deferring to the
     * system's current night-mode setting.
     */
    private fun isDarkTheme(config: DatePickerConfig, context: android.content.Context): Boolean =
        when (config.theme) {
            DatePickerTheme.DARK -> true
            DatePickerTheme.LIGHT -> false
            DatePickerTheme.AUTO, null -> isSystemInDarkMode(context)
        }

    /** Resolve a style field: `style.X` ?? theme-derived default. */
    private fun effectiveTextColor(config: DatePickerConfig, context: android.content.Context): String {
        config.style?.textColor?.let { return it }
        return if (isDarkTheme(config, context)) "#ffffff" else "#000000"
    }

    private fun effectiveDividerColor(config: DatePickerConfig, context: android.content.Context): String {
        config.style?.dividerColor?.let { return it }
        // A muted grey that reads on both light and dark backgrounds.
        return if (isDarkTheme(config, context)) "#666666" else "#cccccc"
    }

    private fun effectiveBackgroundColor(config: DatePickerConfig, context: android.content.Context): Int {
        config.style?.backgroundColor?.let { hex ->
            try { return Color.parseColor(hex) } catch (e: IllegalArgumentException) { /* fall through */ }
        }
        return if (isDarkTheme(config, context)) Color.parseColor("#1c1c1e") else Color.WHITE
    }

    private fun defaultTitle(mode: DatePickerMode): String =
        if (mode == DatePickerMode.TIME) "Select time" else "Select date"
}

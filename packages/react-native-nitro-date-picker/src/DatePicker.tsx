import React, { useCallback, useEffect, useRef } from 'react'
import { nitroDatePicker } from './specs/NitroDatePicker.nitro'
import type { DatePickerConfig, PickerStyle } from './specs/NitroDatePicker.nitro'
import type {
  DatePickerMode,
  DatePickerTheme,
  HourSource,
} from './specs/NitroDatePicker.nitro'

/** Allowed minute steps for the minutes wheel. */
export type MinuteInterval = 1 | 2 | 3 | 4 | 5 | 6 | 10 | 12 | 15 | 20 | 30

export interface DatePickerProps {
  /** Whether the picker is visible */
  open: boolean

  /** Currently selected date */
  date: Date

  /** Picker mode */
  mode?: DatePickerMode

  /** Locale identifier, e.g. 'en_US' */
  locale?: string

  /** Minimum selectable date */
  minimumDate?: Date

  /** Maximum selectable date */
  maximumDate?: Date

  /** Minute granularity */
  minuteInterval?: MinuteInterval

  /** Force timezone offset in minutes. undefined = device timezone */
  timeZoneOffsetInMinutes?: number

  /** Theme — semantic intent that drives style defaults when `style` doesn't override. */
  theme?: DatePickerTheme

  /**
   * Explicit visual style overrides. Each set field wins over the `theme`-derived default.
   * Most fields are Android-only (iOS `UIDatePicker` is nearly opaque); see `PickerStyle` docs.
   */
  style?: PickerStyle

  /** 12/24 hour format source */
  is24HourSource?: HourSource

  /** Confirm button text */
  confirmText?: string

  /** Cancel button text */
  cancelText?: string

  /** Modal title */
  title?: string | null

  /** Called when date changes */
  onDateChange?: (date: Date) => void

  /** Called when user confirms */
  onConfirm?: (date: Date) => void

  /** Called when user cancels */
  onCancel?: () => void
}

/**
 * Renders a native date/time picker dialog.
 *
 * The picker is backed by a single process-global Nitro HybridObject, which means **only one
 * `<DatePicker>` should be `open` at a time**. Mounting two simultaneously (or across overlapping
 * screens) will clobber the `onDateChange`/`onConfirm`/`onCancel` callbacks of the first. This
 * matches the typical modal-usage pattern and is the same constraint the underlying
 * `UIDatePicker`/`AlertDialog` APIs imply.
 */
export function DatePicker({
  open,
  date,
  mode = 'datetime',
  locale,
  minimumDate,
  maximumDate,
  minuteInterval = 1,
  timeZoneOffsetInMinutes,
  theme,
  style,
  is24HourSource,
  confirmText,
  cancelText,
  title,
  onDateChange,
  onConfirm,
  onCancel,
}: DatePickerProps): React.JSX.Element | null {
  const prevOpen = useRef(false)

  // Keep the latest config in a ref so the open/close effect below can read the current values
  // without re-running (and re-showing the picker) on every prop change. While the picker is open,
  // prop changes are intentionally ignored — the user is mid-interaction and re-presenting would
  // dismiss their in-progress selection. This mirrors henninghall's behaviour.
  const configRef = useRef<DatePickerConfig>(null!)
  // Build the Nitro config directly as a typed object literal — the compiler checks every field
  // against `DatePickerConfig`, so there's no positional-argument footgun to mis-order.
  configRef.current = {
    date: date.getTime(),
    mode,
    locale,
    minimumDate: minimumDate?.getTime(),
    maximumDate: maximumDate?.getTime(),
    minuteInterval,
    timeZoneOffsetInMinutes,
    theme,
    style,
    is24HourSource,
    confirmText,
    cancelText,
    title: title ?? undefined,
  }

  const handleDateChange = useCallback(
    (timestamp: number) => {
      onDateChange?.(new Date(timestamp))
    },
    [onDateChange]
  )

  const handleConfirm = useCallback(
    (timestamp: number) => {
      onConfirm?.(new Date(timestamp))
    },
    [onConfirm]
  )

  const handleCancel = useCallback(() => {
    onCancel?.()
  }, [onCancel])

  // Sync callbacks whenever they change.
  useEffect(() => {
    nitroDatePicker.onDateChange = handleDateChange
    nitroDatePicker.onConfirm = handleConfirm
    nitroDatePicker.onCancel = handleCancel

    return () => {
      nitroDatePicker.onDateChange = undefined
      nitroDatePicker.onConfirm = undefined
      nitroDatePicker.onCancel = undefined
    }
  }, [handleDateChange, handleConfirm, handleCancel])

  // Show / dismiss only on the `open` transition. Reading the latest config from a ref (instead of
  // the dep array) means a prop change while the picker is already open does NOT re-present the
  // dialog — the user's in-progress selection is preserved.
  useEffect(() => {
    if (open && !prevOpen.current) {
      nitroDatePicker.show(configRef.current)
    } else if (!open && prevOpen.current) {
      nitroDatePicker.dismiss()
    }
    prevOpen.current = open
    // Intentionally only depends on `open` — see comment above.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open])

  return null
}

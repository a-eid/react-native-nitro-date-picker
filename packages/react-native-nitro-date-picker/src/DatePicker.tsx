import React, { useCallback, useEffect, useRef } from 'react'
import { nitroDatePicker } from './specs/NitroDatePicker.nitro'
import type { DatePickerConfig, DatePickerMode, DatePickerTheme, HourSource } from './specs/NitroDatePicker.nitro'

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
  minuteInterval?: 1 | 2 | 3 | 4 | 5 | 6 | 10 | 12 | 15 | 20 | 30

  /** Force timezone offset in minutes. undefined = device timezone */
  timeZoneOffsetInMinutes?: number

  /** Theme */
  theme?: DatePickerTheme

  /** Text color as hex */
  textColor?: string

  /** Divider color (Android only) */
  dividerColor?: string

  /** Button color (Android modal) */
  buttonColor?: string

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

function toTimestamp(date: Date): number {
  return date.getTime()
}

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
  textColor,
  dividerColor,
  buttonColor,
  is24HourSource,
  confirmText,
  cancelText,
  title,
  onDateChange,
  onConfirm,
  onCancel,
}: DatePickerProps): React.JSX.Element | null {
  const prevOpen = useRef(false)

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

  // Sync callbacks
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

  // Show / dismiss based on open prop
  useEffect(() => {
    if (open && !prevOpen.current) {
      const config: DatePickerConfig = {
        date: toTimestamp(date),
        mode,
        locale,
        minimumDate: minimumDate ? toTimestamp(minimumDate) : undefined,
        maximumDate: maximumDate ? toTimestamp(maximumDate) : undefined,
        minuteInterval,
        timeZoneOffsetInMinutes,
        theme,
        textColor,
        dividerColor,
        buttonColor,
        is24HourSource,
        confirmText,
        cancelText,
        title: title === null ? undefined : title,
      }
      nitroDatePicker.show(config)
    } else if (!open && prevOpen.current) {
      nitroDatePicker.dismiss()
    }
    prevOpen.current = open
  }, [
    open,
    date,
    mode,
    locale,
    minimumDate,
    maximumDate,
    minuteInterval,
    timeZoneOffsetInMinutes,
    theme,
    textColor,
    dividerColor,
    buttonColor,
    is24HourSource,
    confirmText,
    cancelText,
    title,
  ])

  return null
}

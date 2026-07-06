import { type HybridObject, NitroModules } from 'react-native-nitro-modules'

export type DatePickerMode = 'date' | 'time' | 'datetime'
export type DatePickerTheme = 'light' | 'dark' | 'auto'
export type HourSource = 'locale' | 'device'

export interface DatePickerConfig {
  date: number
  mode: DatePickerMode
  locale?: string
  minimumDate?: number
  maximumDate?: number
  minuteInterval: number
  timeZoneOffsetInMinutes?: number
  theme?: DatePickerTheme
  textColor?: string
  dividerColor?: string
  buttonColor?: string
  is24HourSource?: HourSource
  confirmText?: string
  cancelText?: string
  title?: string
}

export interface NitroDatePicker extends HybridObject<{ ios: 'swift'; android: 'kotlin' }> {
  show(config: DatePickerConfig): void
  dismiss(): void
  onDateChange: ((timestamp: number) => void) | undefined
  onConfirm: ((timestamp: number) => void) | undefined
  onCancel: (() => void) | undefined
}

const nitroDatePicker = NitroModules.createHybridObject<NitroDatePicker>('NitroDatePicker')
export { nitroDatePicker }

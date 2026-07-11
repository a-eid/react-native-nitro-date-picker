import { type HybridObject, NitroModules } from 'react-native-nitro-modules'

export type DatePickerMode = 'date' | 'time' | 'datetime'
export type DatePickerTheme = 'light' | 'dark' | 'auto'
export type HourSource = 'locale' | 'device'

/**
 * Visual styling for the picker wheels. All fields are optional; any unset field falls back to the
 * `theme`-derived default.
 *
 * Fields marked **(Android only)** have no effect on iOS — `UIDatePicker` does not expose them.
 * Fields marked **(cross-platform)** work on both platforms.
 *
 * Units: `height`, `dividerThickness`, `dividerDistance`, `itemSpacing` are in dp; `textSize` and
 * `selectedTextSize` are in sp; colors are CSS hex strings (`#rgb`, `#rrggbb`, `#aarrggbb`).
 */
export interface PickerStyle {
  /** (cross-platform) Wheel text color. */
  textColor?: string
  /** (cross-platform) Background color behind the wheels. */
  backgroundColor?: string
  /** (cross-platform) Wheel area height in dp. */
  height?: number
  /** (Android only) Color of the selected row. Defaults to `textColor`. */
  selectedTextColor?: string
  /** (Android only) Font size of non-selected rows, in sp. */
  textSize?: number
  /** (Android only) Font size of the selected row, in sp. */
  selectedTextSize?: number
  /** (Android only) Color of the two selection divider lines. */
  dividerColor?: string
  /** (Android only) Thickness of the divider lines, in dp. */
  dividerThickness?: number
  /** (Android only) Vertical distance between the two divider lines, in dp. */
  dividerDistance?: number
  /** (Android only) Extra vertical gap between the selected row and its neighbours, in dp. */
  itemSpacing?: number
  /** (Android only) Number of rows shown on each wheel (min 3). */
  wheelItemCount?: number
}

export interface DatePickerConfig {
  date: number
  mode: DatePickerMode
  locale?: string
  minimumDate?: number
  maximumDate?: number
  minuteInterval: number
  timeZoneOffsetInMinutes?: number
  theme?: DatePickerTheme
  /** Explicit visual style overrides. Each set field wins over the `theme`-derived default. */
  style?: PickerStyle
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

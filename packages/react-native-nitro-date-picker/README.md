# react-native-nitro-date-picker

A fast, minimal, type-safe date picker for React Native built with [Nitro Modules](https://nitro.margelo.com/).

A Nitro-based reimplementation of [henninghall/react-native-date-picker](https://github.com/henninghall/react-native-date-picker) — the same signature **spinner/wheel** UI and props, powered by Nitro's JSI-backed hybrid objects.

Under the hood it renders native `UIDatePicker` (`.wheels`) on iOS and a row of `NumberPicker` spinner wheels on Android, with locale-aware wheel labels and ordering. Web is supported via a fallback modal.

## Installation

```bash
npm install react-native-nitro-date-picker
# or
yarn add react-native-nitro-date-picker
# or
bun add react-native-nitro-date-picker
```

## Usage

```tsx
import { useState } from 'react'
import { Button } from 'react-native'
import { DatePicker } from 'react-native-nitro-date-picker'

function App() {
  const [date, setDate] = useState(new Date())
  const [open, setOpen] = useState(false)

  return (
    <>
      <Button title="Pick a date" onPress={() => setOpen(true)} />
      <DatePicker
        open={open}
        date={date}
        mode="datetime"
        onDateChange={setDate}
        onConfirm={(d) => {
          setDate(d)
          setOpen(false)
        }}
        onCancel={() => setOpen(false)}
      />
    </>
  )
}
```

## Props

| Prop | Type | Default | Description |
|---|---|---|---|
| `open` | `boolean` | **required** | Controls visibility |
| `date` | `Date` | **required** | Currently selected date |
| `mode` | `'date' \| 'time' \| 'datetime'` | `'datetime'` | Picker mode |
| `locale` | `string` | device default | e.g. `'en_US'` |
| `minimumDate` | `Date` | — | Lower bound |
| `maximumDate` | `Date` | — | Upper bound |
| `minuteInterval` | `1 \| 2 \| 3 \| 4 \| 5 \| 6 \| 10 \| 12 \| 15 \| 20 \| 30` | `1` | Minute granularity |
| `timeZoneOffsetInMinutes` | `number` | device timezone | Force timezone offset |
| `theme` | `'light' \| 'dark' \| 'auto'` | `'auto'` | Drives style defaults when `style` doesn't override |
| `style` | `PickerStyle` | — | Explicit visual overrides (see below) |
| `is24HourSource` | `'locale' \| 'device'` | platform default | 12/24h format |
| `confirmText` | `string` | `'Confirm'` | Confirm button label |
| `cancelText` | `string` | `'Cancel'` | Cancel button label |
| `title` | `string \| null` | auto | Modal title. `null` hides it |
| `onDateChange` | `(date: Date) => void` | — | Fires on every change |
| `onConfirm` | `(date: Date) => void` | — | Fires when user confirms |
| `onCancel` | `() => void` | — | Fires when user cancels |

### `PickerStyle`

All fields optional; each set field overrides the `theme`-derived default. Fields marked **(Android)**
have no effect on iOS — `UIDatePicker` does not expose them.

| Field | Type | Platform | Description |
|---|---|---|---|
| `textColor` | `string` | both | Wheel text color (hex) |
| `backgroundColor` | `string` | both | Background behind the wheels (hex) |
| `height` | `number` | both | Wheel area height (dp) |
| `selectedTextColor` | `string` | Android | Selected row color (defaults to `textColor`) |
| `textSize` | `number` | Android | Non-selected row font size (sp) |
| `selectedTextSize` | `number` | Android | Selected row font size (sp) |
| `dividerColor` | `string` | Android | Selection divider line color (hex) |
| `dividerThickness` | `number` | Android | Divider thickness (dp) |
| `dividerDistance` | `number` | Android | Distance between the two dividers (dp) |
| `itemSpacing` | `number` | Android | Gap between selected row and neighbours (dp) |
| `wheelItemCount` | `number` | Android | Rows per wheel (min 3) |

## Advanced: Direct Nitro API

For full type safety and direct access to the underlying Nitro HybridObject:

```tsx
import { nitroDatePicker } from 'react-native-nitro-date-picker'
import type { DatePickerConfig } from 'react-native-nitro-date-picker'

nitroDatePicker.onDateChange = (timestamp: number) => console.log(new Date(timestamp))
nitroDatePicker.onConfirm = (timestamp: number) => console.log('confirmed')

const config: DatePickerConfig = {
  date: Date.now(),
  mode: 'datetime',
  minuteInterval: 5,
  theme: 'dark',
}

nitroDatePicker.show(config)
// Later: nitroDatePicker.dismiss()
```

## Platform Notes

- **iOS**: Native `UIDatePicker` (`.wheels` style) embedded in a `UIAlertController` action sheet.
  Arbitrary `textColor` is applied via KVC.
- **Android**: A row of `NumberPicker` spinner wheels (`year`, `month`, `date`, `day`, `hour`,
  `minutes`, `ampm`) inside an `AlertDialog`. Only the wheels relevant to the `mode` are shown, and
  their left-to-right order is derived from the locale's date-time pattern. Mirrors henninghall's
  architecture, so invalid dates (e.g. April 31) snap back to the nearest valid date, and min/max
  bounds are enforced by animating the wheels back.
- **Web**: Renders the native `<input type="date|time|datetime-local">` inside a modal. Backdrop
  click and `Esc` dismiss the picker.

> **One at a time.** The picker is backed by a single process-global Nitro hybrid object. Only one
> `<DatePicker>` should be `open` at a time; mounting two simultaneously will clobber the first one's
> callbacks.

## Acknowledgements

- [henninghall/react-native-date-picker](https://github.com/henninghall/react-native-date-picker) —
  the original library whose spinner UX and design this project reproduces.
- [react-native-nitro-modules](https://github.com/mrousavy/nitro) — the Nitro framework.
- [ShawnLin013/NumberPicker](https://github.com/ShawnLin013/NumberPicker) — the Android wheel widget.

## License

MIT

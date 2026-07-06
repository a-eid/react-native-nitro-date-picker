# react-native-nitro-date-picker

A fast, minimal, type-safe date picker for React Native built with [Nitro Modules](https://nitro.margelo.com/).

Under the hood it uses native `UIDatePicker` on iOS and `DatePicker`/`TimePicker` on Android — no custom wheel rendering, no old bridge overhead.

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
| `theme` | `'light' \| 'dark' \| 'auto'` | `'auto'` | Color theme |
| `textColor` | `string` | — | Hex color |
| `dividerColor` | `string` | — | Android only |
| `buttonColor` | `string` | — | Android only |
| `is24HourSource` | `'locale' \| 'device'` | platform default | 12/24h format |
| `confirmText` | `string` | `'Confirm'` | Confirm button label |
| `cancelText` | `string` | `'Cancel'` | Cancel button label |
| `title` | `string \| null` | auto | Modal title. `null` hides it |
| `onDateChange` | `(date: Date) => void` | — | Fires on every change |
| `onConfirm` | `(date: Date) => void` | — | Fires when user confirms |
| `onCancel` | `() => void` | — | Fires when user cancels |

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

- **iOS**: Uses native `UIDatePicker` with wheel style embedded in a `UIAlertController` action sheet
- **Android**: Uses native `DatePicker` and `TimePicker` widgets inside an `AlertDialog`

## License

MIT

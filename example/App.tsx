import { useState } from 'react'
import {
  Button,
  Platform,
  StyleSheet,
  Text,
  View,
} from 'react-native'
import { DatePicker } from 'react-native-nitro-date-picker'
import type { DatePickerTheme, PickerStyle } from 'react-native-nitro-date-picker'

type Mode = 'date' | 'time' | 'datetime'

// Style presets to demonstrate JS-side styling. Tweak these live — no native rebuild needed.
const stylePresets: { label: string; value: PickerStyle | undefined }[] = [
  { label: 'default', value: undefined },
  { label: 'compact', value: { height: 140, textSize: 14, selectedTextSize: 14, itemSpacing: 2, dividerDistance: 18 } },
  { label: 'spacious', value: { height: 220, textSize: 20, selectedTextSize: 22, itemSpacing: 10, dividerDistance: 28 } },
  { label: 'accent', value: { textColor: '#007AFF', selectedTextColor: '#000000', dividerColor: '#007AFF', dividerThickness: 2 } },
]

export default function App() {
  const [date, setDate] = useState(new Date())
  const [open, setOpen] = useState(false)
  const [mode, setMode] = useState<Mode>('datetime')
  const [theme, setTheme] = useState<DatePickerTheme>('auto')
  const [stylePreset, setStylePreset] = useState(0)
  const [displayed, setDisplayed] = useState<Date | null>(null)

  const formatDate = (d: Date, m: string) => {
    if (m === 'time') return d.toLocaleTimeString()
    return `${d.toLocaleDateString()} ${d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}`
  }

  const modes: Mode[] = ['date', 'time', 'datetime']
  const themes: { label: string; value: DatePickerTheme }[] = [
    { label: 'auto', value: 'auto' },
    { label: 'light', value: 'light' },
    { label: 'dark', value: 'dark' },
  ]

  const activeStyle = stylePresets[stylePreset]?.value

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Nitro Date Picker</Text>

      {displayed && (
        <Text style={styles.selected}>
          {formatDate(displayed, mode)}
        </Text>
      )}

      <View style={styles.section}>
        <Text style={styles.sectionLabel}>Mode</Text>
        <View style={styles.modeRow}>
          {modes.map((m) => (
            <View key={m} style={styles.modeButton}>
              <Button
                title={m}
                color={mode === m ? '#007AFF' : '#8E8E93'}
                onPress={() => setMode(m)}
              />
            </View>
          ))}
        </View>
      </View>

      <View style={styles.section}>
        <Text style={styles.sectionLabel}>Theme (picker)</Text>
        <View style={styles.modeRow}>
          {themes.map((t) => (
            <View key={t.value} style={styles.modeButton}>
              <Button
                title={t.label}
                color={theme === t.value ? '#007AFF' : '#8E8E93'}
                onPress={() => setTheme(t.value)}
              />
            </View>
          ))}
        </View>
      </View>

      <View style={styles.section}>
        <Text style={styles.sectionLabel}>Style</Text>
        <View style={styles.modeRow}>
          {stylePresets.map((s, i) => (
            <View key={s.label} style={styles.modeButton}>
              <Button
                title={s.label}
                color={stylePreset === i ? '#007AFF' : '#8E8E93'}
                onPress={() => setStylePreset(i)}
              />
            </View>
          ))}
        </View>
      </View>

      <Button title="Open Date Picker" onPress={() => setOpen(true)} />

      <DatePicker
        open={open}
        date={date}
        mode={mode}
        theme={theme}
        style={activeStyle}
        onDateChange={(d: Date) => setDate(d)}
        onConfirm={(d: Date) => {
          setDate(d)
          setDisplayed(d)
          setOpen(false)
        }}
        onCancel={() => setOpen(false)}
      />

      <Text style={styles.note}>
        {Platform.OS === 'ios'
          ? 'Using native UIDatePicker ⚡'
          : 'Using native spinner wheels ⚡'}
      </Text>
    </View>
  )
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: 24,
    backgroundColor: '#F2F2F7',
  },
  title: {
    fontSize: 28,
    fontWeight: '700',
    marginBottom: 24,
    color: '#1C1C1E',
  },
  selected: {
    fontSize: 18,
    marginBottom: 24,
    color: '#3A3A3C',
    textAlign: 'center',
  },
  section: {
    marginBottom: 20,
    alignItems: 'center',
  },
  sectionLabel: {
    fontSize: 13,
    fontWeight: '600',
    color: '#8E8E93',
    marginBottom: 8,
    textTransform: 'uppercase',
    letterSpacing: 0.5,
  },
  modeRow: {
    flexDirection: 'row',
    gap: 8,
  },
  modeButton: {
    flex: 1,
    minWidth: 64,
  },
  note: {
    position: 'absolute',
    bottom: 60,
    fontSize: 13,
    color: '#8E8E93',
  },
})

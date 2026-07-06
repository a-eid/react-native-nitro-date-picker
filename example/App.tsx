import { useState } from 'react'
import {
  Button,
  Platform,
  StyleSheet,
  Text,
  View,
} from 'react-native'
import { DatePicker } from 'react-native-nitro-date-picker'

type Mode = 'date' | 'time' | 'datetime'

export default function App() {
  const [date, setDate] = useState(new Date())
  const [open, setOpen] = useState(false)
  const [mode, setMode] = useState<Mode>('datetime')
  const [displayed, setDisplayed] = useState<Date | null>(null)

  const formatDate = (d: Date, m: string) => {
    if (m === 'time') return d.toLocaleTimeString()
    return `${d.toLocaleDateString()} ${d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}`
  }

  const modes: Mode[] = ['date', 'time', 'datetime']

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Nitro Date Picker</Text>

      {displayed && (
        <Text style={styles.selected}>{formatDate(displayed, mode)}</Text>
      )}

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

      <Button title="Open Date Picker" onPress={() => setOpen(true)} />

      <DatePicker
        open={open}
        date={date}
        mode={mode}
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
          : 'Using native DatePicker/TimePicker ⚡'}
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
  modeRow: {
    flexDirection: 'row',
    marginBottom: 24,
    gap: 8,
  },
  modeButton: {
    flex: 1,
  },
  note: {
    position: 'absolute',
    bottom: 60,
    fontSize: 13,
    color: '#8E8E93',
  },
})

import React, { useState } from 'react'
import {
  Button,
  StyleSheet,
  Text,
  View,
  SafeAreaView,
} from 'react-native'
import { DatePicker } from 'react-native-nitro-date-picker'

export default function App() {
  const [date, setDate] = useState(new Date())
  const [open, setOpen] = useState(false)
  const [mode, setMode] = useState<'date' | 'time' | 'datetime'>('datetime')
  const [displayedDate, setDisplayedDate] = useState<Date | null>(null)

  const formatDate = (d: Date, m: string): string => {
    if (m === 'time') {
      return d.toLocaleTimeString()
    }
    return d.toLocaleDateString() + ' ' + d.toLocaleTimeString()
  }

  return (
    <SafeAreaView style={styles.container}>
      <Text style={styles.title}>Nitro Date Picker</Text>

      {displayedDate && (
        <Text style={styles.selectedDate}>
          {formatDate(displayedDate, mode)}
        </Text>
      )}

      <View style={styles.modeRow}>
        {(['date', 'time', 'datetime'] as const).map((m) => (
          <View key={m} style={styles.modeBtn}>
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
        onDateChange={(d) => setDate(d)}
        onConfirm={(d) => {
          setDate(d)
          setDisplayedDate(d)
          setOpen(false)
        }}
        onCancel={() => {
          setOpen(false)
        }}
      />
    </SafeAreaView>
  )
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: 20,
    backgroundColor: '#F2F2F7',
  },
  title: {
    fontSize: 28,
    fontWeight: '700',
    marginBottom: 24,
    color: '#1C1C1E',
  },
  selectedDate: {
    fontSize: 18,
    marginBottom: 24,
    color: '#3A3A3C',
    textAlign: 'center',
    paddingHorizontal: 20,
  },
  modeRow: {
    flexDirection: 'row',
    marginBottom: 24,
    gap: 8,
  },
  modeBtn: {
    flex: 1,
  },
})

import React, { useCallback, useEffect, useRef, useState } from 'react';
import { Modal, Pressable, StyleSheet, Text, View } from 'react-native';
import type { DatePickerProps } from './DatePicker';

type Mode = 'date' | 'time' | 'datetime';

function pad(n: number): string {
  return n < 10 ? `0${n}` : `${n}`;
}

function toInputValue(date: Date, mode: Mode): string {
  const y = date.getFullYear();
  const m = pad(date.getMonth() + 1);
  const d = pad(date.getDate());
  const h = pad(date.getHours());
  const min = pad(date.getMinutes());
  switch (mode) {
    case 'date':
      return `${y}-${m}-${d}`;
    case 'time':
      return `${h}:${min}`;
    case 'datetime':
      return `${y}-${m}-${d}T${h}:${min}`;
  }
}

function fromInputValue(value: string, mode: Mode): Date | null {
  if (!value) return null;
  switch (mode) {
    case 'date': {
      const parts = value.split('-').map(Number);
      const [y, mo, d] = parts;
      if (y == null || mo == null || d == null || !Number.isFinite(y) || !Number.isFinite(mo) || !Number.isFinite(d)) {
        return null;
      }
      return new Date(y, mo - 1, d);
    }
    case 'time': {
      const parts = value.split(':').map(Number);
      const [h, min] = parts;
      if (h == null || min == null || !Number.isFinite(h) || !Number.isFinite(min)) {
        return null;
      }
      const d = new Date();
      d.setHours(h, min, 0, 0);
      return d;
    }
    case 'datetime': {
      const parsed = new Date(value);
      return Number.isNaN(parsed.getTime()) ? null : parsed;
    }
  }
}

/**
 * Web fallback for `<DatePicker>`.
 *
 * Renders the native `<input type="date|time|datetime-local">` inside a small modal. Because the
 * native flow emits `onDateChange` continuously and then `onConfirm` with the final value, we keep
 * a local copy of the in-progress date so `onConfirm` reports the value the user actually sees,
 * not the stale `date` prop.
 */
export function DatePicker({
  open,
  date,
  mode = 'datetime',
  minimumDate,
  maximumDate,
  minuteInterval = 1,
  theme,
  confirmText = 'Confirm',
  cancelText = 'Cancel',
  title,
  onDateChange,
  onConfirm,
  onCancel,
}: DatePickerProps): React.JSX.Element | null {
  const inputRef = useRef<HTMLInputElement>(null);
  // The live value as the user edits it. Re-seeded from `date` whenever the picker (re)opens.
  const [liveDate, setLiveDate] = useState(date);

  useEffect(() => {
    if (open) setLiveDate(date);
  }, [open, date]);

  const inputType = mode === 'datetime' ? 'datetime-local' : mode;

  const handleChange = useCallback(
    (e: unknown) => {
      const event = e as { target: { value: string } };
      const parsed = fromInputValue(event.target.value, mode);
      if (parsed) {
        setLiveDate(parsed);
        onDateChange?.(parsed);
      }
    },
    [mode, onDateChange]
  );

  const handleConfirm = useCallback(() => {
    // Report the value the user is actually looking at, not the prop we opened with.
    onConfirm?.(liveDate);
  }, [liveDate, onConfirm]);

  // Esc key dismisses the modal, matching native back-button cancel behaviour.
  useEffect(() => {
    if (!open) return;
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onCancel?.();
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [open, onCancel]);

  if (!open) return null;

  const isDark = theme === 'dark';

  const inputStyle = {
    fontSize: 18,
    padding: 12,
    border: isDark ? '1px solid #555' : '1px solid #ccc',
    borderRadius: 8,
    marginBottom: 16,
    width: '100%',
    backgroundColor: isDark ? '#333' : '#fff',
    color: isDark ? '#fff' : '#000',
  };

  const inputProps: Record<string, unknown> = {
    type: inputType,
    value: toInputValue(liveDate, mode),
    onChange: handleChange,
    style: inputStyle,
  };

  if (minimumDate) inputProps.min = toInputValue(minimumDate, mode);
  if (maximumDate) inputProps.max = toInputValue(maximumDate, mode);
  if (mode === 'time') inputProps.step = minuteInterval * 60;

  return (
    <Modal transparent visible={open} animationType="fade" onRequestClose={onCancel}>
      <Pressable style={styles.overlay} onPress={onCancel}>
        <Pressable
          style={[styles.container, isDark && styles.containerDark]}
          onPress={(e) => e.stopPropagation()}
        >
          {title !== null && (
            <Text style={[styles.title, isDark && styles.titleDark]}>
              {title ?? (mode === 'time' ? 'Select time' : 'Select date')}
            </Text>
          )}

          {/* @ts-ignore web-only input element */}
          <input ref={inputRef} {...inputProps} />

          <View style={styles.buttonRow}>
            <Pressable
              style={({ pressed }) => [
                styles.button,
                styles.cancelButton,
                pressed && styles.pressed,
              ]}
              onPress={onCancel}
            >
              <Text style={styles.cancelButtonText}>{cancelText}</Text>
            </Pressable>
            <Pressable
              style={({ pressed }) => [
                styles.button,
                styles.confirmButton,
                pressed && styles.pressed,
              ]}
              onPress={handleConfirm}
            >
              <Text style={styles.confirmButtonText}>{confirmText}</Text>
            </Pressable>
          </View>
        </Pressable>
      </Pressable>
    </Modal>
  );
}

const styles = StyleSheet.create({
  overlay: {
    flex: 1,
    backgroundColor: 'rgba(0, 0, 0, 0.4)',
    justifyContent: 'center',
    alignItems: 'center',
  },
  container: {
    backgroundColor: '#fff',
    borderRadius: 14,
    padding: 20,
    marginHorizontal: 32,
    width: 320,
    maxWidth: '90%',
  },
  containerDark: {
    backgroundColor: '#1c1c1e',
  },
  title: {
    fontSize: 17,
    fontWeight: '600',
    textAlign: 'center',
    marginBottom: 16,
    color: '#000',
  },
  titleDark: {
    color: '#fff',
  },
  buttonRow: {
    flexDirection: 'row',
    justifyContent: 'flex-end',
    gap: 12,
  },
  button: {
    paddingVertical: 10,
    paddingHorizontal: 20,
    borderRadius: 8,
  },
  cancelButton: {
    backgroundColor: '#e5e5ea',
  },
  cancelButtonText: {
    fontSize: 16,
    color: '#007aff',
    fontWeight: '600',
  },
  confirmButton: {
    backgroundColor: '#007aff',
  },
  confirmButtonText: {
    fontSize: 16,
    color: '#fff',
    fontWeight: '600',
  },
  pressed: {
    opacity: 0.7,
  },
});

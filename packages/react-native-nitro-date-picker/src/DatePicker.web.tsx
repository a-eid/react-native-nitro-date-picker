import React, { useCallback, useEffect, useRef } from 'react';
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
      const [y, mo, d] = value.split('-').map(Number);
      if (y == null || mo == null || d == null) return null;
      return new Date(y, mo - 1, d);
    }
    case 'time': {
      const [h, min] = value.split(':').map(Number);
      if (h == null || min == null) return null;
      const d = new Date();
      d.setHours(h, min, 0, 0);
      return d;
    }
    case 'datetime': {
      return new Date(value);
    }
  }
}

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
  const mountedRef = useRef(false);

  const inputType =
    mode === 'datetime'
      ? 'datetime-local'
      : mode;

  const handleChange = useCallback(
    (e: unknown) => {
      const event = e as { target: { value: string } };
      const parsed = fromInputValue(event.target.value, mode);
      if (parsed) onDateChange?.(parsed);
    },
    [mode, onDateChange]
  );

  const handleConfirm = useCallback(() => {
    onConfirm?.(date);
  }, [date, onConfirm]);

  const setInputRef = useCallback(
    (el: unknown) => {
      (inputRef as { current: { focus?: () => void } | null }).current =
        el as { focus?: () => void } | null;
    },
    []
  );

  useEffect(() => {
    if (open && !mountedRef.current) {
      mountedRef.current = true;
      setTimeout(() => {
        inputRef.current?.focus();
      }, 100);
    }
    if (!open) {
      mountedRef.current = false;
    }
  }, [open]);

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
    value: toInputValue(date, mode),
    onChange: handleChange,
    style: inputStyle,
  };

  if (minimumDate) inputProps.min = toInputValue(minimumDate, mode);
  if (maximumDate) inputProps.max = toInputValue(maximumDate, mode);
  if (mode === 'time') inputProps.step = minuteInterval * 60;

  return (
    <Modal transparent visible={open} animationType="fade">
      <View style={styles.overlay}>
        <View style={[styles.container, isDark && styles.containerDark]}>
          {title !== null && (
            <Text style={[styles.title, isDark && styles.titleDark]}>
              {title ?? (mode === 'time' ? 'Select time' : 'Select date')}
            </Text>
          )}

          {/* @ts-ignore web-only input element */}
          <input ref={setInputRef} {...inputProps} />

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
        </View>
      </View>
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

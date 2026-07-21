import type { CSSProperties } from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { useThemeColors } from '@/hooks/use-theme-colors';
import { useColorScheme } from '@/hooks/use-color-scheme';
import { SimpleDateRange } from '@/utils/date-range';

interface DateRangePickerProps {
  range: SimpleDateRange;
  onChange: (range: SimpleDateRange) => void;
}

/**
 * Web build of `DateRangePicker` — `@react-native-community/datetimepicker`
 * has no web implementation, so this uses the browser's native
 * `<input type="date">` instead (same `.web.tsx` platform-split convention
 * as `hooks/use-color-scheme.web.ts`).
 */
export function DateRangePicker({ range, onChange }: DateRangePickerProps) {
  const colors = useThemeColors();
  const scheme = useColorScheme() ?? 'light';
  // Native <input type="date"> renders its own calendar popup using the
  // browser's `color-scheme` — without this it stays light and its text
  // disappears against a dark page background.
  const inputStyle: CSSProperties = {
    fontSize: 14,
    fontWeight: 700,
    color: colors.text,
    border: 'none',
    outline: 'none',
    fontFamily: 'inherit',
    background: 'transparent',
    width: '100%',
    colorScheme: scheme,
  };

  return (
    <View style={styles.row}>
      <View style={[styles.field, { backgroundColor: colors.card, borderColor: colors.border }]}>
        <Text style={[styles.label, { color: colors.textSecondary }]}>Desde</Text>
        <input
          type="date"
          value={range.from}
          max={range.to}
          onChange={e => onChange({ from: e.target.value, to: range.to })}
          style={inputStyle}
        />
      </View>
      <View style={[styles.field, { backgroundColor: colors.card, borderColor: colors.border }]}>
        <Text style={[styles.label, { color: colors.textSecondary }]}>Hasta</Text>
        <input
          type="date"
          value={range.to}
          min={range.from}
          max={new Date().toISOString().slice(0, 10)}
          onChange={e => onChange({ from: range.from, to: e.target.value })}
          style={inputStyle}
        />
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  row: { flexDirection: 'row', gap: 10, paddingHorizontal: 16, paddingBottom: 12 },
  field: {
    flex: 1,
    borderRadius: 12,
    borderWidth: 1,
    paddingVertical: 8,
    paddingHorizontal: 12,
    gap: 2,
  },
  label: { fontSize: 11, fontWeight: '600', textTransform: 'uppercase', letterSpacing: 0.4 },
});

import { Pressable, StyleSheet, Text, View } from 'react-native';
import { Brand } from '@/constants/theme';
import { useThemeColors } from '@/hooks/use-theme-colors';
import { QUICK_RANGE_LABELS, QuickRange } from '@/utils/date-range';

interface QuickRangeChipsProps {
  value: QuickRange;
  onChange: (range: QuickRange) => void;
  /** Defaults to the four fixed presets. Pass `DEFAULT_RANGES.concat('custom')`
   * on screens that also offer `DateRangePicker` for an explicit range. */
  ranges?: QuickRange[];
}

export const DEFAULT_RANGES: QuickRange[] = ['today', 'yesterday', 'week', 'month'];

/** Date-range filter as tappable chips instead of a native date picker for
 * the four fixed presets — a phone report screen only really needs those
 * most of the time. Screens that need an arbitrary range add a
 * `'custom'` chip (via `ranges`) that switches them to `DateRangePicker`. */
export function QuickRangeChips({ value, onChange, ranges = DEFAULT_RANGES }: QuickRangeChipsProps) {
  const colors = useThemeColors();
  return (
    <View style={styles.row}>
      {ranges.map(range => {
        const active = range === value;
        return (
          <Pressable
            key={range}
            onPress={() => onChange(range)}
            style={[
              styles.chip,
              { backgroundColor: colors.card, borderColor: colors.border },
              active && styles.chipActive,
            ]}
          >
            <Text style={[styles.chipText, { color: colors.textSecondary }, active && styles.chipTextActive]}>
              {QUICK_RANGE_LABELS[range]}
            </Text>
          </Pressable>
        );
      })}
    </View>
  );
}

const styles = StyleSheet.create({
  row: { flexDirection: 'row', gap: 8, paddingHorizontal: 16, paddingVertical: 12 },
  chip: {
    paddingVertical: 8,
    paddingHorizontal: 14,
    borderRadius: 20,
    borderWidth: 1,
  },
  chipActive: { backgroundColor: Brand.orange, borderColor: Brand.orange },
  chipText: { fontSize: 13, fontWeight: '600' },
  chipTextActive: { color: '#fff' },
});

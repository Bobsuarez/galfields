import { Pressable, StyleSheet, Text, View } from 'react-native';
import { Brand } from '@/constants/theme';
import { QUICK_RANGE_LABELS, QuickRange } from '@/utils/date-range';

interface QuickRangeChipsProps {
  value: QuickRange;
  onChange: (range: QuickRange) => void;
}

const RANGES: QuickRange[] = ['today', 'yesterday', 'week', 'month'];

/** Date-range filter as tappable chips instead of a native date picker —
 * the project has no date-picker dependency installed yet, and a phone
 * report screen only really needs these four presets. */
export function QuickRangeChips({ value, onChange }: QuickRangeChipsProps) {
  return (
    <View style={styles.row}>
      {RANGES.map(range => {
        const active = range === value;
        return (
          <Pressable
            key={range}
            onPress={() => onChange(range)}
            style={[styles.chip, active && styles.chipActive]}
          >
            <Text style={[styles.chipText, active && styles.chipTextActive]}>
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
    backgroundColor: '#fff',
    borderWidth: 1,
    borderColor: '#E8DDD0',
  },
  chipActive: { backgroundColor: Brand.orange, borderColor: Brand.orange },
  chipText: { fontSize: 13, fontWeight: '600', color: '#8A7060' },
  chipTextActive: { color: '#fff' },
});

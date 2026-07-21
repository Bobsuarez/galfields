import { useState } from 'react';
import { Modal, Platform, Pressable, StyleSheet, Text, View } from 'react-native';
import DateTimePicker, { DateTimePickerEvent } from '@react-native-community/datetimepicker';
import { Brand } from '@/constants/theme';
import { useThemeColors } from '@/hooks/use-theme-colors';
import { useColorScheme } from '@/hooks/use-color-scheme';
import { parseIsoLocal, SimpleDateRange, toIsoLocal } from '@/utils/date-range';

interface DateRangePickerProps {
  range: SimpleDateRange;
  onChange: (range: SimpleDateRange) => void;
}

type Field = 'from' | 'to';

/**
 * "Desde"/"Hasta" fields backed by the native date picker, for the
 * "Personalizado" chip on report screens (see `useReportDateRange`).
 * Android's picker is already a self-dismissing system dialog, so it's
 * rendered directly; iOS's `inline` mode has no dismiss affordance of its
 * own (it just sits there firing `onChange` on every tap), so it's wrapped
 * in a small modal with an explicit "Listo" button instead.
 */
export function DateRangePicker({ range, onChange }: DateRangePickerProps) {
  const colors = useThemeColors();
  const scheme = useColorScheme() ?? 'light';
  const [openField, setOpenField] = useState<Field | null>(null);

  function commit(field: Field, date: Date) {
    const iso = toIsoLocal(date);
    onChange(field === 'from' ? { from: iso, to: range.to } : { from: range.from, to: iso });
  }

  function handleAndroidChange(field: Field) {
    return (event: DateTimePickerEvent, date?: Date) => {
      setOpenField(null);
      if (event.type === 'set' && date) commit(field, date);
    };
  }

  function handleIosChange(field: Field) {
    return (_event: DateTimePickerEvent, date?: Date) => {
      if (date) commit(field, date);
    };
  }

  return (
    <View style={styles.row}>
      <Pressable
        style={[styles.field, { backgroundColor: colors.card, borderColor: colors.border }]}
        onPress={() => setOpenField('from')}
      >
        <Text style={[styles.label, { color: colors.textSecondary }]}>Desde</Text>
        <Text style={[styles.value, { color: colors.text }]}>{range.from}</Text>
      </Pressable>
      <Pressable
        style={[styles.field, { backgroundColor: colors.card, borderColor: colors.border }]}
        onPress={() => setOpenField('to')}
      >
        <Text style={[styles.label, { color: colors.textSecondary }]}>Hasta</Text>
        <Text style={[styles.value, { color: colors.text }]}>{range.to}</Text>
      </Pressable>

      {Platform.OS === 'android' && openField && (
        <DateTimePicker
          value={parseIsoLocal(range[openField])}
          mode="date"
          maximumDate={new Date()}
          onChange={handleAndroidChange(openField)}
        />
      )}

      {Platform.OS !== 'android' && (
        <Modal visible={openField !== null} transparent animationType="fade" onRequestClose={() => setOpenField(null)}>
          <Pressable style={[styles.backdrop, { backgroundColor: colors.overlay }]} onPress={() => setOpenField(null)}>
            <Pressable style={[styles.sheet, { backgroundColor: colors.card }]}>
              {openField && (
                <DateTimePicker
                  value={parseIsoLocal(range[openField])}
                  mode="date"
                  display="inline"
                  maximumDate={new Date()}
                  themeVariant={scheme}
                  onChange={handleIosChange(openField)}
                />
              )}
              <Pressable style={styles.doneButton} onPress={() => setOpenField(null)}>
                <Text style={styles.doneText}>Listo</Text>
              </Pressable>
            </Pressable>
          </Pressable>
        </Modal>
      )}
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
  value: { fontSize: 14, fontWeight: '700' },
  backdrop: { flex: 1, justifyContent: 'flex-end' },
  sheet: { borderTopLeftRadius: 16, borderTopRightRadius: 16, paddingBottom: 20, alignItems: 'center' },
  doneButton: { marginTop: 4, paddingVertical: 10, paddingHorizontal: 28, backgroundColor: Brand.orange, borderRadius: 20 },
  doneText: { color: '#fff', fontSize: 14, fontWeight: '700' },
});

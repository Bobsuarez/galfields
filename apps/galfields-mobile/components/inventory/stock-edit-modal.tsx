import { useEffect, useState } from 'react';
import { KeyboardAvoidingView, Modal, Platform, Pressable, StyleSheet, Text, View } from 'react-native';
import { AppButton } from '@/components/ui/app-button';
import { TextInputField } from '@/components/ui/text-input-field';
import { Colors } from '@/constants/theme';
import { useColorScheme } from '@/hooks/use-color-scheme';
import type { InventoryVariantRow } from './inventory-variant-item';

interface StockEditModalProps {
  row: InventoryVariantRow | null;
  saving?: boolean;
  onSave: (newStock: number) => void;
  onCancel: () => void;
}

/**
 * Takes the count the user physically did ("hay 42 unidades"), not a
 * +/- delta — computes `delta = newStock - row.stock` here and lets the
 * caller report just that delta to POST /api/inventory/adjustments (see
 * services/inventory-api.ts), which only understands relative deltas.
 */
export function StockEditModal({ row, saving, onSave, onCancel }: StockEditModalProps) {
  const scheme = useColorScheme() ?? 'light';
  const colors = Colors[scheme];
  const [value, setValue] = useState('');
  const [error, setError] = useState('');

  useEffect(() => {
    if (row) {
      setValue(String(row.stock));
      setError('');
    }
  }, [row]);

  const handleSave = () => {
    const parsed = Number(value);
    if (value.trim() === '' || Number.isNaN(parsed) || !Number.isInteger(parsed) || parsed < 0) {
      setError('Ingresa una cantidad válida (0 o más)');
      return;
    }
    onSave(parsed);
  };

  return (
    <Modal visible={!!row} transparent animationType="fade" onRequestClose={onCancel}>
      <Pressable style={styles.backdrop} onPress={onCancel} />
      <KeyboardAvoidingView behavior={Platform.OS === 'ios' ? 'padding' : undefined} style={styles.sheetWrap}>
        <View style={[styles.sheet, { backgroundColor: colors.card }]}>
          <Text style={[styles.title, { color: colors.text }]} numberOfLines={2}>
            {row?.displayName}
          </Text>
          <Text style={[styles.currentStock, { color: colors.textSecondary }]}>Stock actual: {row?.stock ?? 0}</Text>

          <TextInputField
            label="Nueva cantidad"
            placeholder="Ej. 42"
            keyboardType="number-pad"
            value={value}
            onChangeText={t => {
              setValue(t);
              setError('');
            }}
            error={error}
          />

          <View style={styles.footer}>
            <View style={styles.footerBtn}>
              <AppButton label="Cancelar" variant="outline" onPress={onCancel} disabled={saving} />
            </View>
            <View style={styles.footerBtn}>
              <AppButton label="Guardar" onPress={handleSave} loading={saving} />
            </View>
          </View>
        </View>
      </KeyboardAvoidingView>
    </Modal>
  );
}

const styles = StyleSheet.create({
  backdrop: { ...StyleSheet.absoluteFillObject, backgroundColor: 'rgba(0,0,0,0.4)' },
  sheetWrap: { position: 'absolute', bottom: 0, left: 0, right: 0 },
  sheet: {
    borderTopLeftRadius: 20,
    borderTopRightRadius: 20,
    paddingTop: 20,
    paddingHorizontal: 20,
    paddingBottom: 32,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: -4 },
    shadowOpacity: 0.12,
    shadowRadius: 12,
    elevation: 20,
  },
  title: { fontSize: 16, fontWeight: '700', marginBottom: 4 },
  currentStock: { fontSize: 13, marginBottom: 16 },
  footer: { flexDirection: 'row', gap: 12, marginTop: 8 },
  footerBtn: { flex: 1 },
});

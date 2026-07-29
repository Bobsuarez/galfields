import { useEffect, useState } from 'react';
import { KeyboardAvoidingView, Modal, Platform, Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { AppButton } from '@/components/ui/app-button';
import { TextInputField } from '@/components/ui/text-input-field';
import { Colors } from '@/constants/theme';
import { useColorScheme } from '@/hooks/use-color-scheme';
import type { ProductUnit, ProductUnitFormData } from '@/services/product-units-api';

interface ProductUnitFormModalProps {
  visible: boolean;
  editing: ProductUnit | null;
  saving?: boolean;
  onSave: (values: ProductUnitFormData) => void;
  onCancel: () => void;
}

export function ProductUnitFormModal({ visible, editing, saving, onSave, onCancel }: ProductUnitFormModalProps) {
  const scheme = useColorScheme() ?? 'light';
  const colors = Colors[scheme];
  const [unitName, setUnitName] = useState('');
  const [conversionFactor, setConversionFactor] = useState('');
  const [unitPrice, setUnitPrice] = useState('');
  const [barcode, setBarcode] = useState('');
  const [errors, setErrors] = useState<{ unitName?: string; conversionFactor?: string; unitPrice?: string }>({});

  // The base unit's factor is always 1 (backend rejects any other value for
  // it) - lock the field instead of letting the user hit a save error.
  const isBase = editing?.isBase ?? false;

  useEffect(() => {
    if (visible) {
      setUnitName(editing?.unitName ?? '');
      setConversionFactor(editing ? String(editing.conversionFactor) : '');
      setUnitPrice(editing ? String(editing.unitPrice) : '');
      setBarcode(editing?.barcode ?? '');
      setErrors({});
    }
  }, [visible, editing]);

  const handleSave = () => {
    const nextErrors: typeof errors = {};
    if (!unitName.trim()) nextErrors.unitName = 'El nombre de la unidad es requerido';
    if (!conversionFactor.trim() || Number(conversionFactor) < 1)
      nextErrors.conversionFactor = 'Ingresa un factor de conversión válido (mínimo 1)';
    if (!unitPrice.trim() || Number(unitPrice) < 0) nextErrors.unitPrice = 'Ingresa un precio válido';

    if (Object.keys(nextErrors).length > 0) {
      setErrors(nextErrors);
      return;
    }

    onSave({
      unitName: unitName.trim(),
      conversionFactor: Number(conversionFactor),
      unitPrice: Number(unitPrice),
      barcode: barcode.trim(),
    });
  };

  return (
    <Modal visible={visible} transparent animationType="fade" onRequestClose={onCancel}>
      <Pressable style={styles.backdrop} onPress={onCancel} />
      <KeyboardAvoidingView behavior={Platform.OS === 'ios' ? 'padding' : undefined} style={styles.sheetWrap}>
        <View style={[styles.sheet, { backgroundColor: colors.card }]}>
          <Text style={[styles.sheetTitle, { color: colors.text }]}>
            {editing ? 'Editar unidad' : 'Nueva unidad de venta'}
          </Text>
          <ScrollView keyboardShouldPersistTaps="handled">
            <TextInputField
              label="Nombre de la unidad"
              placeholder="Ej. Media, Completa, Cartón"
              value={unitName}
              onChangeText={t => {
                setUnitName(t);
                setErrors(prev => ({ ...prev, unitName: undefined }));
              }}
              error={errors.unitName}
            />
            <TextInputField
              label="Factor de conversión"
              placeholder="Ej. 10"
              value={conversionFactor}
              onChangeText={t => {
                setConversionFactor(t.replace(/[^0-9]/g, ''));
                setErrors(prev => ({ ...prev, conversionFactor: undefined }));
              }}
              keyboardType="numeric"
              editable={!isBase}
              style={isBase ? { color: colors.textSecondary } : undefined}
              error={errors.conversionFactor}
            />
            {isBase ? (
              <Text style={[styles.hint, { color: colors.textSecondary }]}>
                La unidad base siempre tiene factor 1 — es la unidad en la que se cuenta el stock.
              </Text>
            ) : null}
            <TextInputField
              label="Precio de venta"
              placeholder="0"
              value={unitPrice}
              onChangeText={t => {
                setUnitPrice(t.replace(/[^0-9]/g, ''));
                setErrors(prev => ({ ...prev, unitPrice: undefined }));
              }}
              keyboardType="numeric"
              error={errors.unitPrice}
            />
            <TextInputField
              label="Código de barras (opcional)"
              placeholder="Ingresa o escanea"
              value={barcode}
              onChangeText={setBarcode}
              keyboardType="number-pad"
            />
          </ScrollView>
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
  backdrop: {
    ...StyleSheet.absoluteFillObject,
    backgroundColor: 'rgba(0,0,0,0.4)',
  },
  sheetWrap: {
    position: 'absolute',
    bottom: 0,
    left: 0,
    right: 0,
  },
  sheet: {
    borderTopLeftRadius: 20,
    borderTopRightRadius: 20,
    paddingTop: 20,
    paddingHorizontal: 20,
    paddingBottom: 32,
    maxHeight: '85%',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: -4 },
    shadowOpacity: 0.12,
    shadowRadius: 12,
    elevation: 20,
  },
  sheetTitle: { fontSize: 16, fontWeight: '700', marginBottom: 16 },
  hint: { fontSize: 12, marginTop: -8, marginBottom: 14 },
  footer: { flexDirection: 'row', gap: 12, marginTop: 16 },
  footerBtn: { flex: 1 },
});

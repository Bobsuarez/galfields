import { useEffect, useState } from 'react';
import {
  KeyboardAvoidingView,
  Modal,
  Platform,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import { AppButton } from '@/components/ui/app-button';
import { TextInputField } from '@/components/ui/text-input-field';
import { ImagePickerField } from '@/components/products/image-picker-field';
import { usePlainImagePicker } from '@/hooks/use-plain-image-picker';
import { Brand, Colors } from '@/constants/theme';
import { useColorScheme } from '@/hooks/use-color-scheme';
import type { PaymentMethod, PaymentMethodFormData } from '@/services/payment-methods-api';

interface PaymentMethodFormModalProps {
  visible: boolean;
  editing: PaymentMethod | null;
  saving?: boolean;
  onSave: (values: PaymentMethodFormData) => void;
  onCancel: () => void;
}

export function PaymentMethodFormModal({
  visible,
  editing,
  saving,
  onSave,
  onCancel,
}: PaymentMethodFormModalProps) {
  const scheme = useColorScheme() ?? 'light';
  const colors = Colors[scheme];
  const [methodName, setMethodName] = useState('');
  const [active, setActive] = useState(true);
  const [previewUri, setPreviewUri] = useState<string | null | undefined>(undefined);
  // Only set once the user actively picks/clears a photo this session — the
  // existing `imageUrl` is a remote URL and can't be re-sent as a file part
  // (see PaymentMethodFormData). Stays undefined = "keep current image".
  const [pickedUri, setPickedUri] = useState<string | null | undefined>(undefined);
  const [error, setError] = useState('');

  const { pick, clear } = usePlainImagePicker(uri => {
    setPickedUri(uri);
    setPreviewUri(uri ?? undefined);
  });

  useEffect(() => {
    if (visible) {
      setMethodName(editing?.name ?? '');
      setActive(editing?.active ?? true);
      setPreviewUri(editing?.imageUrl ?? undefined);
      setPickedUri(undefined);
      setError('');
    }
  }, [visible, editing]);

  const handleSave = () => {
    if (!methodName.trim()) {
      setError('El nombre es requerido');
      return;
    }
    onSave({ methodName: methodName.trim(), active, imageUri: pickedUri });
  };

  return (
    <Modal visible={visible} transparent animationType="fade" onRequestClose={onCancel}>
      <Pressable style={styles.backdrop} onPress={onCancel} />
      <KeyboardAvoidingView
        behavior={Platform.OS === 'ios' ? 'padding' : undefined}
        style={styles.sheetWrap}
      >
        <View style={[styles.sheet, { backgroundColor: colors.card }]}>
          <Text style={[styles.sheetTitle, { color: colors.text }]}>
            {editing ? 'Editar método de pago' : 'Nuevo método de pago'}
          </Text>
          <ScrollView keyboardShouldPersistTaps="handled">
            <TextInputField
              label="Nombre"
              placeholder="Ej. Nequi"
              value={methodName}
              onChangeText={t => {
                setMethodName(t);
                setError('');
              }}
              error={error}
            />

            <Text style={[styles.fieldLabel, { color: colors.text }]}>Estado</Text>
            <View style={styles.statusRow}>
              <Pressable
                onPress={() => setActive(true)}
                style={[styles.statusOption, active && styles.statusOptionActive]}
              >
                <Text style={[styles.statusText, active && styles.statusTextActive]}>Activo</Text>
              </Pressable>
              <Pressable
                onPress={() => setActive(false)}
                style={[styles.statusOption, !active && styles.statusOptionInactive]}
              >
                <Text style={[styles.statusText, !active && styles.statusTextInactive]}>Inactivo</Text>
              </Pressable>
            </View>

            <Text style={[styles.fieldLabel, { color: colors.text }]}>Imagen (opcional)</Text>
            <ImagePickerField imageUri={previewUri} processing={false} onPick={pick} onRemove={clear} />
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
  fieldLabel: { fontSize: 13, fontWeight: '500', marginBottom: 6 },
  statusRow: { flexDirection: 'row', gap: 10, marginBottom: 14 },
  statusOption: {
    flex: 1,
    alignItems: 'center',
    paddingVertical: 10,
    borderRadius: 10,
    borderWidth: 1,
    borderColor: '#E8DDD0',
  },
  statusOptionActive: { backgroundColor: `${Brand.success}1A`, borderColor: Brand.success },
  statusOptionInactive: { backgroundColor: `${Brand.danger}14`, borderColor: Brand.danger },
  statusText: { fontSize: 14, fontWeight: '600', color: '#8A7060' },
  statusTextActive: { color: Brand.success },
  statusTextInactive: { color: Brand.danger },
  footer: { flexDirection: 'row', gap: 12, marginTop: 16 },
  footerBtn: { flex: 1 },
});

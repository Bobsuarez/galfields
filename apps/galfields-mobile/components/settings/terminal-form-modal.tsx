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
import { Brand, Colors } from '@/constants/theme';
import { useColorScheme } from '@/hooks/use-color-scheme';
import type { Terminal, TerminalFormData } from '@/services/terminals-api';

interface TerminalFormModalProps {
  visible: boolean;
  editing: Terminal | null;
  saving?: boolean;
  onSave: (values: TerminalFormData) => void;
  onCancel: () => void;
}

export function TerminalFormModal({ visible, editing, saving, onSave, onCancel }: TerminalFormModalProps) {
  const scheme = useColorScheme() ?? 'light';
  const colors = Colors[scheme];
  const [terminalCode, setTerminalCode] = useState('');
  const [name, setName] = useState('');
  const [active, setActive] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    if (visible) {
      setTerminalCode(editing?.terminalCode ?? '');
      setName(editing?.name ?? '');
      setActive(editing?.active ?? true);
      setError('');
    }
  }, [visible, editing]);

  const handleSave = () => {
    if (!terminalCode.trim()) {
      setError('El código de terminal es requerido');
      return;
    }
    onSave({ terminalCode: terminalCode.trim(), name: name.trim(), active });
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
            {editing ? 'Editar terminal' : 'Nueva terminal'}
          </Text>
          <ScrollView keyboardShouldPersistTaps="handled">
            <TextInputField
              label="Código de terminal"
              placeholder="Ej. CAJA-01"
              value={terminalCode}
              onChangeText={t => {
                setTerminalCode(t);
                setError('');
              }}
              autoCapitalize="characters"
              autoCorrect={false}
              error={error}
            />
            <TextInputField
              label="Nombre (opcional)"
              placeholder="Ej. Caja principal"
              value={name}
              onChangeText={setName}
            />

            <Text style={[styles.fieldLabel, { color: colors.text }]}>Estado</Text>
            <View style={styles.statusRow}>
              <Pressable
                onPress={() => setActive(true)}
                style={[styles.statusOption, { borderColor: colors.border }, active && styles.statusOptionActive]}
              >
                <Text style={[styles.statusText, { color: colors.textSecondary }, active && styles.statusTextActive]}>
                  Activa
                </Text>
              </Pressable>
              <Pressable
                onPress={() => setActive(false)}
                style={[styles.statusOption, { borderColor: colors.border }, !active && styles.statusOptionInactive]}
              >
                <Text style={[styles.statusText, { color: colors.textSecondary }, !active && styles.statusTextInactive]}>
                  Inactiva
                </Text>
              </Pressable>
            </View>
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
  },
  statusOptionActive: { backgroundColor: `${Brand.success}1A`, borderColor: Brand.success },
  statusOptionInactive: { backgroundColor: `${Brand.danger}14`, borderColor: Brand.danger },
  statusText: { fontSize: 14, fontWeight: '600' },
  statusTextActive: { color: Brand.success },
  statusTextInactive: { color: Brand.danger },
  footer: { flexDirection: 'row', gap: 12, marginTop: 16 },
  footerBtn: { flex: 1 },
});

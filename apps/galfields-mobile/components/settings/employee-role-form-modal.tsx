import { useEffect, useState } from 'react';
import {
  KeyboardAvoidingView,
  Modal,
  Platform,
  Pressable,
  ScrollView,
  StyleSheet,
  Switch,
  Text,
  View,
} from 'react-native';
import { AppButton } from '@/components/ui/app-button';
import { TextInputField } from '@/components/ui/text-input-field';
import { Brand, Colors } from '@/constants/theme';
import { useColorScheme } from '@/hooks/use-color-scheme';
import { PERMISSION_MODULES, type EmployeeRole, type EmployeeRoleFormData } from '@/services/employee-roles-api';

interface EmployeeRoleFormModalProps {
  visible: boolean;
  editing: EmployeeRole | null;
  saving?: boolean;
  onSave: (values: EmployeeRoleFormData) => void;
  onCancel: () => void;
}

function emptyPermissions(): Record<string, boolean> {
  return Object.fromEntries(PERMISSION_MODULES.map(m => [m.key, false]));
}

export function EmployeeRoleFormModal({ visible, editing, saving, onSave, onCancel }: EmployeeRoleFormModalProps) {
  const scheme = useColorScheme() ?? 'light';
  const colors = Colors[scheme];
  const [roleName, setRoleName] = useState('');
  const [permissions, setPermissions] = useState<Record<string, boolean>>(emptyPermissions());
  const [canLoginMobile, setCanLoginMobile] = useState(false);
  const [canLoginDesktop, setCanLoginDesktop] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (visible) {
      setRoleName(editing?.roleName ?? '');
      setPermissions({ ...emptyPermissions(), ...editing?.permissions });
      setCanLoginMobile(editing?.canLoginMobile ?? false);
      setCanLoginDesktop(editing?.canLoginDesktop ?? false);
      setError('');
    }
  }, [visible, editing]);

  const handleSave = () => {
    if (!roleName.trim()) {
      setError('El nombre del rol es requerido');
      return;
    }
    onSave({ roleName: roleName.trim(), permissions, canLoginMobile, canLoginDesktop });
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
            {editing ? 'Editar rol' : 'Nuevo rol'}
          </Text>
          <ScrollView keyboardShouldPersistTaps="handled">
            <TextInputField
              label="Nombre del rol"
              placeholder="Ej. Cajero"
              value={roleName}
              onChangeText={t => {
                setRoleName(t);
                setError('');
              }}
              error={error}
            />

            <Text style={[styles.fieldLabel, { color: colors.text }]}>Permisos por módulo</Text>
            {PERMISSION_MODULES.map(module => (
              <View key={module.key} style={styles.switchRow}>
                <Text style={[styles.switchLabel, { color: colors.text }]}>{module.label}</Text>
                <Switch
                  value={permissions[module.key] ?? false}
                  onValueChange={v => setPermissions(prev => ({ ...prev, [module.key]: v }))}
                  trackColor={{ true: Brand.orange }}
                />
              </View>
            ))}

            <Text style={[styles.fieldLabel, { color: colors.text }]}>Acceso</Text>
            <View style={styles.switchRow}>
              <Text style={[styles.switchLabel, { color: colors.text }]}>Puede loguear en mobile</Text>
              <Switch value={canLoginMobile} onValueChange={setCanLoginMobile} trackColor={{ true: Brand.orange }} />
            </View>
            <View style={styles.switchRow}>
              <Text style={[styles.switchLabel, { color: colors.text }]}>Puede loguear en el POS de escritorio</Text>
              <Switch value={canLoginDesktop} onValueChange={setCanLoginDesktop} trackColor={{ true: Brand.orange }} />
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
  fieldLabel: { fontSize: 13, fontWeight: '500', marginTop: 8, marginBottom: 6 },
  switchRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingVertical: 8,
  },
  switchLabel: { fontSize: 14, flex: 1, marginRight: 12 },
  footer: { flexDirection: 'row', gap: 12, marginTop: 16 },
  footerBtn: { flex: 1 },
});

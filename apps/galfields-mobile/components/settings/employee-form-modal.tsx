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
import type { Employee, EmployeeFormData } from '@/services/employees-api';
import type { EmployeeRole } from '@/services/employee-roles-api';
import type { Terminal } from '@/services/terminals-api';

interface EmployeeFormModalProps {
  visible: boolean;
  editing: Employee | null;
  roles: EmployeeRole[];
  terminals: Terminal[];
  saving?: boolean;
  onSave: (values: EmployeeFormData) => void;
  onCancel: () => void;
}

export function EmployeeFormModal({
  visible,
  editing,
  roles,
  terminals,
  saving,
  onSave,
  onCancel,
}: EmployeeFormModalProps) {
  const scheme = useColorScheme() ?? 'light';
  const colors = Colors[scheme];
  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [roleId, setRoleId] = useState<number | null>(null);
  const [terminalIds, setTerminalIds] = useState<number[]>([]);
  const [errors, setErrors] = useState<Record<string, string>>({});

  useEffect(() => {
    if (visible) {
      setFirstName(editing?.firstName ?? '');
      setLastName(editing?.lastName ?? '');
      setUsername(editing?.username ?? '');
      setPassword('');
      setRoleId(editing?.roleId ?? null);
      setTerminalIds(editing?.terminalIds ?? []);
      setErrors({});
    }
  }, [visible, editing]);

  const toggleTerminal = (id: number) => {
    setTerminalIds(prev => (prev.includes(id) ? prev.filter(t => t !== id) : [...prev, id]));
  };

  const handleSave = () => {
    const nextErrors: Record<string, string> = {};
    if (!firstName.trim()) nextErrors.firstName = 'El nombre es requerido';
    if (!lastName.trim()) nextErrors.lastName = 'El apellido es requerido';
    if (!username.trim()) nextErrors.username = 'El usuario es requerido';
    if (!editing && !password.trim()) nextErrors.password = 'La clave es requerida';
    if (roleId == null) nextErrors.role = 'Selecciona un rol';
    setErrors(nextErrors);
    if (Object.keys(nextErrors).length > 0) return;

    onSave({
      firstName: firstName.trim(),
      lastName: lastName.trim(),
      username: username.trim(),
      // Blank on edit = keep the current password (see EmployeeFormData).
      password: password.trim() ? password : undefined,
      roleId: roleId as number,
      terminalIds,
    });
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
            {editing ? 'Editar empleado' : 'Nuevo empleado'}
          </Text>
          <ScrollView keyboardShouldPersistTaps="handled">
            <TextInputField
              label="Nombre"
              placeholder="Ej. Juan"
              value={firstName}
              onChangeText={t => {
                setFirstName(t);
                setErrors(prev => ({ ...prev, firstName: '' }));
              }}
              error={errors.firstName}
            />
            <TextInputField
              label="Apellido"
              placeholder="Ej. Pérez"
              value={lastName}
              onChangeText={t => {
                setLastName(t);
                setErrors(prev => ({ ...prev, lastName: '' }));
              }}
              error={errors.lastName}
            />
            <TextInputField
              label="Usuario"
              placeholder="Ej. jperez"
              value={username}
              onChangeText={t => {
                setUsername(t);
                setErrors(prev => ({ ...prev, username: '' }));
              }}
              autoCapitalize="none"
              autoCorrect={false}
              error={errors.username}
            />
            <TextInputField
              label={editing ? 'Nueva clave (opcional)' : 'Clave'}
              placeholder={editing ? 'Dejar en blanco para no cambiarla' : 'Clave inicial'}
              value={password}
              onChangeText={t => {
                setPassword(t);
                setErrors(prev => ({ ...prev, password: '' }));
              }}
              secureTextEntry
              error={errors.password}
            />

            <Text style={[styles.fieldLabel, { color: colors.text }]}>Rol</Text>
            {errors.role ? <Text style={styles.errorText}>{errors.role}</Text> : null}
            <View style={styles.chipRow}>
              {roles.map(role => {
                const selected = role.id === roleId;
                return (
                  <Pressable
                    key={role.id}
                    onPress={() => {
                      setRoleId(role.id);
                      setErrors(prev => ({ ...prev, role: '' }));
                    }}
                    style={[styles.chip, { borderColor: colors.border }, selected && styles.chipSelected]}
                  >
                    <Text style={[styles.chipText, { color: colors.textSecondary }, selected && styles.chipTextSelected]}>
                      {role.roleName}
                    </Text>
                  </Pressable>
                );
              })}
            </View>

            <Text style={[styles.fieldLabel, { color: colors.text }]}>Terminales asignadas</Text>
            <View style={styles.chipRow}>
              {terminals.length === 0 ? (
                <Text style={[styles.helperText, { color: colors.textSecondary }]}>
                  No hay terminales creadas — solo necesarias para roles con acceso al POS de escritorio.
                </Text>
              ) : (
                terminals.map(terminal => {
                  const selected = terminalIds.includes(terminal.id);
                  return (
                    <Pressable
                      key={terminal.id}
                      onPress={() => toggleTerminal(terminal.id)}
                      style={[styles.chip, { borderColor: colors.border }, selected && styles.chipSelected]}
                    >
                      <Text style={[styles.chipText, { color: colors.textSecondary }, selected && styles.chipTextSelected]}>
                        {terminal.terminalCode}
                      </Text>
                    </Pressable>
                  );
                })
              )}
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
  errorText: { fontSize: 12, color: Brand.danger, marginBottom: 6 },
  helperText: { fontSize: 12, marginBottom: 8 },
  chipRow: { flexDirection: 'row', flexWrap: 'wrap', gap: 8, marginBottom: 8 },
  chip: {
    paddingHorizontal: 12,
    paddingVertical: 8,
    borderRadius: 20,
    borderWidth: 1,
  },
  chipSelected: { backgroundColor: `${Brand.orange}1A`, borderColor: Brand.orange },
  chipText: { fontSize: 13, fontWeight: '500' },
  chipTextSelected: { color: Brand.orange, fontWeight: '700' },
  footer: { flexDirection: 'row', gap: 12, marginTop: 16 },
  footerBtn: { flex: 1 },
});

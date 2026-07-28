import { useEffect, useState } from 'react';
import { Alert, FlatList, Pressable, StyleSheet, Text, View } from 'react-native';
import { router } from 'expo-router';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { EmployeeListItem } from './employee-list-item';
import { EmployeeFormModal } from './employee-form-modal';
import { IconSymbol } from '@/components/ui/icon-symbol';
import { Brand } from '@/constants/theme';
import { useThemeColors } from '@/hooks/use-theme-colors';
import { useCatalogCrud } from '@/hooks/use-catalog-crud';
import { employeesApi, type Employee, type EmployeeFormData } from '@/services/employees-api';
import { employeeRolesApi, type EmployeeRole } from '@/services/employee-roles-api';
import { terminalsApi, type Terminal } from '@/services/terminals-api';

export function EmployeesScreen() {
  const { items, loading, error, saving, reload, create, update, remove } = useCatalogCrud(employeesApi);
  const colors = useThemeColors();
  const insets = useSafeAreaInsets();
  const [formVisible, setFormVisible] = useState(false);
  const [editing, setEditing] = useState<Employee | null>(null);

  // Populate the role/terminal pickers inside the form modal - fetched once
  // here (not inside the modal) since the modal is otherwise a plain
  // controlled component, same convention as the rest of this screen family.
  const [roles, setRoles] = useState<EmployeeRole[]>([]);
  const [terminals, setTerminals] = useState<Terminal[]>([]);

  useEffect(() => {
    employeeRolesApi.list().then(setRoles).catch(() => {});
    terminalsApi.list().then(setTerminals).catch(() => {});
  }, []);

  const openCreate = () => {
    setEditing(null);
    setFormVisible(true);
  };

  const openEdit = (item: Employee) => {
    setEditing(item);
    setFormVisible(true);
  };

  const handleSave = async (values: EmployeeFormData) => {
    try {
      if (editing) {
        await update(editing.id, values);
      } else {
        await create(values);
      }
      setFormVisible(false);
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'Error desconocido';
      Alert.alert('No se pudo guardar', msg, [{ text: 'OK' }]);
    }
  };

  const confirmDeactivate = (item: Employee) => {
    Alert.alert('Desactivar', `¿Desactivar a "${item.firstName} ${item.lastName}"? Ya no podrá iniciar sesión.`, [
      { text: 'Cancelar', style: 'cancel' },
      {
        text: 'Desactivar',
        style: 'destructive',
        onPress: async () => {
          try {
            await remove(item.id);
          } catch (err) {
            const msg = err instanceof Error ? err.message : 'Error desconocido';
            Alert.alert('No se pudo desactivar', msg, [{ text: 'OK' }]);
          }
        },
      },
    ]);
  };

  return (
    <View style={[styles.container, { backgroundColor: colors.background }]}>
      <View style={[styles.header, { paddingTop: insets.top + 8 }]}>
        <Pressable onPress={() => router.back()} hitSlop={10}>
          <IconSymbol name="arrow.left" size={24} color="#fff" />
        </Pressable>
        <Text style={styles.headerTitle}>Empleados</Text>
        <Pressable onPress={openCreate} hitSlop={10}>
          <IconSymbol name="plus" size={24} color="#fff" />
        </Pressable>
      </View>

      {error ? (
        <View style={styles.errorBanner}>
          <Text style={styles.errorText}>{error}</Text>
        </View>
      ) : null}

      <FlatList
        data={items}
        keyExtractor={item => String(item.id)}
        refreshing={loading}
        onRefresh={reload}
        renderItem={({ item }) => (
          <EmployeeListItem
            fullName={`${item.firstName} ${item.lastName}`}
            username={item.username}
            roleName={item.roleName}
            active={item.active}
            onEdit={() => openEdit(item)}
            onDelete={() => confirmDeactivate(item)}
          />
        )}
        contentContainerStyle={items.length === 0 && styles.emptyContent}
        ListEmptyComponent={
          !loading ? (
            <View style={styles.empty}>
              <IconSymbol name="person.fill" size={48} color={colors.border} />
              <Text style={[styles.emptyText, { color: colors.textSecondary }]}>No hay empleados registrados</Text>
            </View>
          ) : null
        }
      />

      <EmployeeFormModal
        visible={formVisible}
        editing={editing}
        roles={roles}
        terminals={terminals}
        saving={saving}
        onSave={handleSave}
        onCancel={() => setFormVisible(false)}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 16,
    paddingBottom: 16,
    backgroundColor: Brand.brown,
    gap: 12,
  },
  headerTitle: {
    flex: 1,
    fontSize: 20,
    fontWeight: '700',
    color: '#fff',
    textAlign: 'center',
  },
  errorBanner: {
    backgroundColor: `${Brand.danger}14`,
    paddingVertical: 10,
    paddingHorizontal: 16,
  },
  errorText: { color: Brand.danger, fontSize: 13 },
  emptyContent: { flex: 1 },
  empty: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    paddingTop: 80,
    gap: 12,
  },
  emptyText: { fontSize: 15 },
});

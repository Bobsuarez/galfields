import { useState } from 'react';
import { Alert, FlatList, Pressable, StyleSheet, Text, View } from 'react-native';
import { router } from 'expo-router';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { CatalogListItem } from './catalog-list-item';
import { CatalogFormModal, type CatalogFormField } from './catalog-form-modal';
import { IconSymbol } from '@/components/ui/icon-symbol';
import { Brand } from '@/constants/theme';
import { useThemeColors } from '@/hooks/use-theme-colors';
import { useCatalogCrud } from '@/hooks/use-catalog-crud';

interface CatalogEntity {
  id: number;
  name: string;
}

interface CatalogCrudApi<T, FormData> {
  list: () => Promise<T[]>;
  create: (data: FormData) => Promise<T>;
  update: (id: number, data: FormData) => Promise<T>;
  remove: (id: number) => Promise<void>;
}

interface CatalogScreenProps<T extends CatalogEntity, FormData> {
  /** Plural, shown in the header, e.g. "Categorías" */
  title: string;
  /** Singular, used in modal titles, e.g. "Categoría" */
  entityLabel: string;
  emptyIcon: string;
  emptyLabel: string;
  fields: CatalogFormField[];
  api: CatalogCrudApi<T, FormData>;
  getSubtitle?: (item: T) => string | undefined;
  toFormValues: (item: T) => Record<string, string>;
  fromFormValues: (values: Record<string, string>) => FormData;
}

/**
 * Shared list/create/edit/delete screen for categories, brands and
 * locations — same list+modal shape, only the fields and entity mapping
 * differ per caller.
 */
export function CatalogScreen<T extends CatalogEntity, FormData>({
  title,
  entityLabel,
  emptyIcon,
  emptyLabel,
  fields,
  api,
  getSubtitle,
  toFormValues,
  fromFormValues,
}: CatalogScreenProps<T, FormData>) {
  const { items, loading, error, saving, reload, create, update, remove } = useCatalogCrud<T, FormData>(api);
  const colors = useThemeColors();
  const insets = useSafeAreaInsets();
  const [formVisible, setFormVisible] = useState(false);
  const [editing, setEditing] = useState<T | null>(null);

  const openCreate = () => {
    setEditing(null);
    setFormVisible(true);
  };

  const openEdit = (item: T) => {
    setEditing(item);
    setFormVisible(true);
  };

  const handleSave = async (values: Record<string, string>) => {
    try {
      const data = fromFormValues(values);
      if (editing) {
        await update(editing.id, data);
      } else {
        await create(data);
      }
      setFormVisible(false);
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'Error desconocido';
      Alert.alert('No se pudo guardar', msg, [{ text: 'OK' }]);
    }
  };

  const confirmDelete = (item: T) => {
    Alert.alert('Eliminar', `¿Eliminar "${item.name}"? Esta acción no se puede deshacer.`, [
      { text: 'Cancelar', style: 'cancel' },
      {
        text: 'Eliminar',
        style: 'destructive',
        onPress: async () => {
          try {
            await remove(item.id);
          } catch (err) {
            const msg = err instanceof Error ? err.message : 'Error desconocido';
            Alert.alert('No se pudo eliminar', msg, [{ text: 'OK' }]);
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
        <Text style={styles.headerTitle}>{title}</Text>
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
          <CatalogListItem
            title={item.name}
            subtitle={getSubtitle?.(item)}
            onEdit={() => openEdit(item)}
            onDelete={() => confirmDelete(item)}
          />
        )}
        contentContainerStyle={items.length === 0 && styles.emptyContent}
        ListEmptyComponent={
          !loading ? (
            <View style={styles.empty}>
              <IconSymbol name={emptyIcon as any} size={48} color={colors.border} />
              <Text style={[styles.emptyText, { color: colors.textSecondary }]}>{emptyLabel}</Text>
            </View>
          ) : null
        }
      />

      <CatalogFormModal
        visible={formVisible}
        title={editing ? `Editar ${entityLabel}` : `Nueva ${entityLabel}`}
        fields={fields}
        initialValues={editing ? toFormValues(editing) : undefined}
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

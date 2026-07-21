import { useState } from 'react';
import { Alert, FlatList, Pressable, StyleSheet, Text, View } from 'react-native';
import { router } from 'expo-router';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { PaymentMethodListItem } from './payment-method-list-item';
import { PaymentMethodFormModal } from './payment-method-form-modal';
import { IconSymbol } from '@/components/ui/icon-symbol';
import { Brand } from '@/constants/theme';
import { useThemeColors } from '@/hooks/use-theme-colors';
import { useCatalogCrud } from '@/hooks/use-catalog-crud';
import { paymentMethodsApi, type PaymentMethod, type PaymentMethodFormData } from '@/services/payment-methods-api';

export function PaymentMethodsScreen() {
  const { items, loading, error, saving, reload, create, update, remove } = useCatalogCrud(paymentMethodsApi);
  const colors = useThemeColors();
  const insets = useSafeAreaInsets();
  const [formVisible, setFormVisible] = useState(false);
  const [editing, setEditing] = useState<PaymentMethod | null>(null);

  const openCreate = () => {
    setEditing(null);
    setFormVisible(true);
  };

  const openEdit = (item: PaymentMethod) => {
    setEditing(item);
    setFormVisible(true);
  };

  const handleSave = async (values: PaymentMethodFormData) => {
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

  const confirmDelete = (item: PaymentMethod) => {
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
        <Text style={styles.headerTitle}>Métodos de pago</Text>
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
          <PaymentMethodListItem
            name={item.name}
            active={item.active}
            imageUrl={item.imageUrl}
            onEdit={() => openEdit(item)}
            onDelete={() => confirmDelete(item)}
          />
        )}
        contentContainerStyle={items.length === 0 && styles.emptyContent}
        ListEmptyComponent={
          !loading ? (
            <View style={styles.empty}>
              <IconSymbol name="creditcard.fill" size={48} color={colors.border} />
              <Text style={[styles.emptyText, { color: colors.textSecondary }]}>No se encontraron métodos de pago</Text>
            </View>
          ) : null
        }
      />

      <PaymentMethodFormModal
        visible={formVisible}
        editing={editing}
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

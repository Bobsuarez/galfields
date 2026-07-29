import { useMemo, useState } from 'react';
import { Alert, FlatList, Pressable, StyleSheet, Text, View } from 'react-native';
import { router } from 'expo-router';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { ProductUnitListItem } from './product-unit-list-item';
import { ProductUnitFormModal } from './product-unit-form-modal';
import { IconSymbol } from '@/components/ui/icon-symbol';
import { Brand } from '@/constants/theme';
import { useThemeColors } from '@/hooks/use-theme-colors';
import { useCatalogCrud } from '@/hooks/use-catalog-crud';
import { productUnitsApi, type ProductUnit, type ProductUnitFormData } from '@/services/product-units-api';

interface ProductUnitsScreenProps {
  productId: string;
  variantId: number;
}

export function ProductUnitsScreen({ productId, variantId }: ProductUnitsScreenProps) {
  // A fresh api object per (productId, variantId) pair, kept referentially
  // stable across re-renders so useCatalogCrud's load effect doesn't refetch
  // on every render (see hooks/use-catalog-crud.ts).
  const api = useMemo(() => productUnitsApi(productId, variantId), [productId, variantId]);
  const { items, loading, error, saving, reload, create, update, remove } = useCatalogCrud(api);
  const colors = useThemeColors();
  const insets = useSafeAreaInsets();
  const [formVisible, setFormVisible] = useState(false);
  const [editing, setEditing] = useState<ProductUnit | null>(null);

  const openCreate = () => {
    setEditing(null);
    setFormVisible(true);
  };

  const openEdit = (item: ProductUnit) => {
    setEditing(item);
    setFormVisible(true);
  };

  const handleSave = async (values: ProductUnitFormData) => {
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

  // Soft-deactivate on the backend, not a real delete - see
  // services/product-units-api.ts and backend/pos's CLAUDE.md.
  const confirmDeactivate = (item: ProductUnit) => {
    Alert.alert(
      'Desactivar unidad',
      `¿Desactivar la unidad "${item.unitName}"? Ya no se podrá vender en el POS hasta reactivarla.`,
      [
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
      ],
    );
  };

  return (
    <View style={[styles.container, { backgroundColor: colors.background }]}>
      <View style={[styles.header, { paddingTop: insets.top + 8 }]}>
        <Pressable onPress={() => router.back()} hitSlop={10}>
          <IconSymbol name="arrow.left" size={24} color="#fff" />
        </Pressable>
        <Text style={styles.headerTitle}>Unidades de venta</Text>
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
          <ProductUnitListItem
            unitName={item.unitName}
            conversionFactor={item.conversionFactor}
            unitPrice={item.unitPrice}
            barcode={item.barcode}
            isBase={item.isBase}
            active={item.active}
            onEdit={() => openEdit(item)}
            onDeactivate={() => confirmDeactivate(item)}
          />
        )}
        contentContainerStyle={items.length === 0 && styles.emptyContent}
        ListEmptyComponent={
          !loading ? (
            <View style={styles.empty}>
              <IconSymbol name="square.stack.3d.up.fill" size={48} color={colors.border} />
              <Text style={[styles.emptyText, { color: colors.textSecondary }]}>
                Esta variante todavía no tiene unidades de venta configuradas
              </Text>
            </View>
          ) : null
        }
      />

      <ProductUnitFormModal
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
    paddingHorizontal: 32,
    gap: 12,
  },
  emptyText: { fontSize: 15, textAlign: 'center' },
});

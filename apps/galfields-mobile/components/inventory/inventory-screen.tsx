import { useEffect, useState } from 'react';
import { ActivityIndicator, Alert, FlatList, StyleSheet, Switch, Text, View } from 'react-native';
import { ReportHeader } from '@/components/reports/report-header';
import { InventoryVariantItem, InventoryVariantRow } from './inventory-variant-item';
import { StockEditModal } from './stock-edit-modal';
import { Brand } from '@/constants/theme';
import { usePaginatedFetch } from '@/hooks/use-paginated-fetch';
import { fetchProducts, activateProduct, deactivateProduct, RemoteProduct } from '@/services/products-api';
import { adjustVariantStock } from '@/services/inventory-api';
import type { Page } from '@/services/reports-api';

/** One row per variant, not per product — each variant has its own
 * sellable barcode/stock, same flattening convention `sync.rs` uses on the
 * desktop POS side. */
function flattenProduct(product: RemoteProduct): InventoryVariantRow[] {
  const multiVariant = product.variants.length > 1;

  return product.variants.map(variant => {
    const attrs = variant.attributes.map(a => a.value).join(' ');
    const displayName = multiVariant && attrs ? `${product.name} - ${attrs}` : product.name;

    return {
      productId: product.productId,
      variantId: variant.variantId,
      displayName,
      sku: variant.sku,
      barcode: variant.barcode,
      stock: variant.stock,
      active: variant.active,
      imageUrl: variant.imageUrl ?? product.imageUrl,
    };
  });
}

export function InventoryScreen() {
  const [showInactive, setShowInactive] = useState(false);
  const [editing, setEditing] = useState<InventoryVariantRow | null>(null);
  const [savingStock, setSavingStock] = useState(false);
  const [togglingProductId, setTogglingProductId] = useState<number | null>(null);

  const { items, loading, loadingMore, error, refresh, loadMore } = usePaginatedFetch<InventoryVariantRow>(
    async (page, size): Promise<Page<InventoryVariantRow>> => {
      const productsPage = await fetchProducts(page, size, showInactive);
      return {
        content: productsPage.content.flatMap(flattenProduct),
        number: productsPage.number,
        totalPages: productsPage.totalPages,
        totalElements: productsPage.totalElements,
      };
    },
  );

  useEffect(() => {
    refresh();
    // Only re-run when the toggle changes - `refresh` is a fresh closure
    // every render (see usePaginatedFetch).
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [showInactive]);

  const handleSaveStock = async (newStock: number) => {
    if (!editing) return;
    const delta = newStock - editing.stock;

    if (delta === 0) {
      setEditing(null);
      return;
    }

    setSavingStock(true);
    try {
      await adjustVariantStock(editing.variantId, delta);
      setEditing(null);
      refresh();
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'Error desconocido';
      Alert.alert('No se pudo ajustar el stock', msg, [{ text: 'OK' }]);
    } finally {
      setSavingStock(false);
    }
  };

  const handleToggleActive = (row: InventoryVariantRow) => {
    const action = row.active ? 'Desactivar' : 'Activar';
    const message = row.active
      ? `¿Desactivar "${row.displayName}"? Ya no aparecerá disponible para venta.`
      : `¿Activar "${row.displayName}"? Volverá a estar disponible para venta.`;

    Alert.alert(action, message, [
      { text: 'Cancelar', style: 'cancel' },
      {
        text: action,
        style: row.active ? 'destructive' : 'default',
        onPress: async () => {
          setTogglingProductId(row.productId);
          try {
            if (row.active) {
              await deactivateProduct(row.productId);
            } else {
              await activateProduct(row.productId);
            }
            refresh();
          } catch (err) {
            const msg = err instanceof Error ? err.message : 'Error desconocido';
            Alert.alert(`No se pudo ${action.toLowerCase()}`, msg, [{ text: 'OK' }]);
          } finally {
            setTogglingProductId(null);
          }
        },
      },
    ]);
  };

  return (
    <View style={styles.container}>
      <ReportHeader title="Inventario" />

      <View style={styles.filterRow}>
        <Text style={styles.filterLabel}>Mostrar desactivados</Text>
        <Switch value={showInactive} onValueChange={setShowInactive} trackColor={{ true: Brand.orange }} />
      </View>

      {error ? (
        <Text style={styles.error}>{error}</Text>
      ) : (
        <FlatList
          style={styles.list}
          data={items}
          keyExtractor={item => String(item.variantId)}
          refreshing={loading}
          onRefresh={refresh}
          onEndReachedThreshold={0.4}
          onEndReached={loadMore}
          renderItem={({ item }) => (
            <InventoryVariantItem
              row={item}
              onPressStock={() => setEditing(item)}
              onToggleActive={() => handleToggleActive(item)}
            />
          )}
          ListFooterComponent={
            loadingMore || togglingProductId !== null ? (
              <ActivityIndicator color={Brand.orange} style={styles.footerLoader} />
            ) : null
          }
          ListEmptyComponent={
            !loading ? <Text style={styles.empty}>No hay productos para mostrar.</Text> : null
          }
        />
      )}

      <StockEditModal row={editing} saving={savingStock} onSave={handleSaveStock} onCancel={() => setEditing(null)} />
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: Brand.cream },
  list: { flex: 1 },
  filterRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 16,
    paddingVertical: 10,
  },
  filterLabel: { fontSize: 13, fontWeight: '600', color: '#8A7060' },
  footerLoader: { marginVertical: 16 },
  error: { color: Brand.danger, fontSize: 14, textAlign: 'center', marginTop: 24 },
  empty: { color: '#8A7060', fontSize: 14, textAlign: 'center', marginTop: 40 },
});

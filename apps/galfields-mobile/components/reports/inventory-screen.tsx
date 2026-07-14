import { ActivityIndicator, FlatList, StyleSheet, Text, View } from 'react-native';
import { ReportHeader } from './report-header';
import { InventoryRowItem } from './inventory-row-item';
import { Brand } from '@/constants/theme';
import { usePaginatedFetch } from '@/hooks/use-paginated-fetch';
import { fetchInventory } from '@/services/reports-api';

export function InventoryScreen() {
  const { items, loading, loadingMore, error, refresh, loadMore } = usePaginatedFetch(fetchInventory);

  return (
    <View style={styles.container}>
      <ReportHeader title="Inventario actual" />
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
          renderItem={({ item }) => <InventoryRowItem row={item} />}
          ListFooterComponent={loadingMore ? <ActivityIndicator color={Brand.orange} style={styles.footerLoader} /> : null}
          ListEmptyComponent={!loading ? <Text style={styles.empty}>No hay productos en inventario.</Text> : null}
        />
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: Brand.cream },
  list: { flex: 1 },
  footerLoader: { marginVertical: 16 },
  error: { color: Brand.danger, fontSize: 14, textAlign: 'center', marginTop: 24 },
  empty: { color: '#8A7060', fontSize: 14, textAlign: 'center', marginTop: 40 },
});

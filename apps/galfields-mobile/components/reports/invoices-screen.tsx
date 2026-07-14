import { useEffect, useState } from 'react';
import { ActivityIndicator, FlatList, StyleSheet, Text, View } from 'react-native';
import { router } from 'expo-router';
import { ReportHeader } from './report-header';
import { QuickRangeChips } from './quick-range-chips';
import { InvoiceListItem } from './invoice-list-item';
import { Brand } from '@/constants/theme';
import { quickRangeDates, QuickRange } from '@/utils/date-range';
import { usePaginatedFetch } from '@/hooks/use-paginated-fetch';
import { fetchInvoices } from '@/services/reports-api';

export function InvoicesScreen() {
  const [range, setRange] = useState<QuickRange>('week');
  const { items, loading, loadingMore, error, refresh, loadMore } = usePaginatedFetch((page, size) =>
    fetchInvoices(quickRangeDates(range), page, size),
  );

  useEffect(() => {
    refresh();
    // Only re-run when the selected quick range changes - `refresh` itself
    // is a fresh closure every render (see usePaginatedFetch).
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [range]);

  return (
    <View style={styles.container}>
      <ReportHeader title="Historial de facturas" />
      <QuickRangeChips value={range} onChange={setRange} />

      {error ? (
        <Text style={styles.error}>{error}</Text>
      ) : (
        <FlatList
          style={styles.list}
          data={items}
          keyExtractor={item => String(item.transactionId)}
          refreshing={loading}
          onRefresh={refresh}
          onEndReachedThreshold={0.4}
          onEndReached={loadMore}
          renderItem={({ item }) => (
            <InvoiceListItem invoice={item} onPress={() => router.push(`/reports/invoices/${item.transactionId}`)} />
          )}
          ListFooterComponent={loadingMore ? <ActivityIndicator color={Brand.orange} style={styles.footerLoader} /> : null}
          ListEmptyComponent={!loading ? <Text style={styles.empty}>No hay facturas en este rango.</Text> : null}
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

import { useCallback, useEffect, useState } from 'react';
import { ActivityIndicator, RefreshControl, ScrollView, StyleSheet, Text, View } from 'react-native';
import { ReportHeader } from './report-header';
import { QuickRangeChips } from './quick-range-chips';
import { BarRow } from './bar-row';
import { Brand } from '@/constants/theme';
import { quickRangeDates, QuickRange } from '@/utils/date-range';
import { fetchSalesByPaymentMethod, PaymentMethodSales } from '@/services/reports-api';

export function SalesByPaymentMethodScreen() {
  const [range, setRange] = useState<QuickRange>('week');
  const [rows, setRows] = useState<PaymentMethodSales[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async (selected: QuickRange) => {
    setLoading(true);
    setError(null);
    try {
      setRows(await fetchSalesByPaymentMethod(quickRangeDates(selected)));
    } catch (err) {
      setError(err instanceof Error ? err.message : 'No se pudo cargar el reporte.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load(range);
  }, [range, load]);

  const maxAmount = rows.reduce((max, row) => Math.max(max, row.totalAmount), 0);

  return (
    <View style={styles.container}>
      <ReportHeader title="Ventas por método de pago" />
      <QuickRangeChips value={range} onChange={setRange} />

      <ScrollView
        contentContainerStyle={styles.content}
        refreshControl={<RefreshControl refreshing={loading} onRefresh={() => load(range)} />}
      >
        {error ? (
          <Text style={styles.error}>{error}</Text>
        ) : loading && rows.length === 0 ? (
          <ActivityIndicator color={Brand.orange} style={styles.loader} />
        ) : rows.length === 0 ? (
          <Text style={styles.empty}>No hay ventas en este rango.</Text>
        ) : (
          rows.map(row => (
            <BarRow
              key={row.paymentMethodId}
              label={row.methodName}
              amount={row.totalAmount}
              maxAmount={maxAmount}
              subtitle={`${row.transactionCount} venta${row.transactionCount === 1 ? '' : 's'}`}
            />
          ))
        )}
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: Brand.cream },
  content: { padding: 16 },
  loader: { marginTop: 40 },
  error: { color: Brand.danger, fontSize: 14, textAlign: 'center', marginTop: 24 },
  empty: { color: '#8A7060', fontSize: 14, textAlign: 'center', marginTop: 40 },
});

import { useCallback, useEffect, useState } from 'react';
import { ActivityIndicator, RefreshControl, ScrollView, StyleSheet, Text, View } from 'react-native';
import { ReportHeader } from './report-header';
import { StatCard } from './stat-card';
import { BarRow } from './bar-row';
import { Brand } from '@/constants/theme';
import { formatCurrency } from '@/utils/currency';
import { fetchSalesByPaymentMethod, PaymentMethodSales } from '@/services/reports-api';

/** "Cierre de caja" as a same-day payment-method summary, not a formal
 * open/close register session — there's no session concept yet (see
 * backend/pos's CLAUDE.md). No date chips: this report is always "today". */
export function CashSummaryScreen() {
  const [rows, setRows] = useState<PaymentMethodSales[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setRows(await fetchSalesByPaymentMethod());
    } catch (err) {
      setError(err instanceof Error ? err.message : 'No se pudo cargar el cierre de caja.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const total = rows.reduce((sum, row) => sum + row.totalAmount, 0);
  const maxAmount = rows.reduce((max, row) => Math.max(max, row.totalAmount), 0);

  return (
    <View style={styles.container}>
      <ReportHeader title="Cierre de caja" />

      <ScrollView
        contentContainerStyle={styles.content}
        refreshControl={<RefreshControl refreshing={loading} onRefresh={load} />}
      >
        {error ? (
          <Text style={styles.error}>{error}</Text>
        ) : loading && rows.length === 0 ? (
          <ActivityIndicator color={Brand.orange} style={styles.loader} />
        ) : (
          <>
            <StatCard label="Total del día" value={formatCurrency(total)} accent />
            <View style={styles.spacer} />
            {rows.length === 0 ? (
              <Text style={styles.empty}>Todavía no hay ventas registradas hoy.</Text>
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
          </>
        )}
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: Brand.cream },
  content: { padding: 16 },
  spacer: { height: 16 },
  loader: { marginTop: 40 },
  error: { color: Brand.danger, fontSize: 14, textAlign: 'center', marginTop: 24 },
  empty: { color: '#8A7060', fontSize: 14, textAlign: 'center', marginTop: 24 },
});

import { useCallback, useEffect, useState } from 'react';
import { ActivityIndicator, RefreshControl, ScrollView, StyleSheet, Text, View } from 'react-native';
import { ReportHeader } from './report-header';
import { DEFAULT_RANGES, QuickRangeChips } from './quick-range-chips';
import { DateRangePicker } from './date-range-picker';
import { BarRow } from './bar-row';
import { Brand } from '@/constants/theme';
import { SimpleDateRange } from '@/utils/date-range';
import { useReportDateRange } from '@/hooks/use-report-date-range';
import { fetchSalesByPaymentMethod, PaymentMethodSales } from '@/services/reports-api';
import { useThemeColors } from '@/hooks/use-theme-colors';

const RANGES = [...DEFAULT_RANGES, 'custom' as const];

export function SalesByPaymentMethodScreen() {
  const colors = useThemeColors();
  const { range, setRange, customRange, setCustomRange, dates } = useReportDateRange('week');
  const [rows, setRows] = useState<PaymentMethodSales[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async (selected: SimpleDateRange) => {
    setLoading(true);
    setError(null);
    try {
      setRows(await fetchSalesByPaymentMethod(selected));
    } catch (err) {
      setError(err instanceof Error ? err.message : 'No se pudo cargar el reporte.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load(dates);
    // Only re-run when the resolved dates change - `load` itself is a
    // stable closure with no dependency on `dates`.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [dates.from, dates.to]);

  const maxAmount = rows.reduce((max, row) => Math.max(max, row.totalAmount), 0);

  return (
    <View style={[styles.container, { backgroundColor: colors.background }]}>
      <ReportHeader title="Ventas por método de pago" />
      <QuickRangeChips value={range} onChange={setRange} ranges={RANGES} />
      {range === 'custom' && <DateRangePicker range={customRange} onChange={setCustomRange} />}

      <ScrollView
        contentContainerStyle={styles.content}
        refreshControl={<RefreshControl refreshing={loading} onRefresh={() => load(dates)} />}
      >
        {error ? (
          <Text style={styles.error}>{error}</Text>
        ) : loading && rows.length === 0 ? (
          <ActivityIndicator color={Brand.orange} style={styles.loader} />
        ) : rows.length === 0 ? (
          <Text style={[styles.empty, { color: colors.textSecondary }]}>No hay ventas en este rango.</Text>
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
  container: { flex: 1 },
  content: { padding: 16 },
  loader: { marginTop: 40 },
  error: { color: Brand.danger, fontSize: 14, textAlign: 'center', marginTop: 24 },
  empty: { fontSize: 14, textAlign: 'center', marginTop: 40 },
});

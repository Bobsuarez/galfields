import { useCallback, useEffect, useState } from 'react';
import { ActivityIndicator, RefreshControl, ScrollView, StyleSheet, Text, View } from 'react-native';
import { ReportHeader } from './report-header';
import { QuickRangeChips } from './quick-range-chips';
import { StatCard } from './stat-card';
import { Brand } from '@/constants/theme';
import { formatCurrency } from '@/utils/currency';
import { quickRangeDates, QuickRange } from '@/utils/date-range';
import { fetchSalesSummary, SalesSummary } from '@/services/reports-api';

export function SalesDailyScreen() {
  const [range, setRange] = useState<QuickRange>('today');
  const [summary, setSummary] = useState<SalesSummary | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async (selected: QuickRange) => {
    setLoading(true);
    setError(null);
    try {
      setSummary(await fetchSalesSummary(quickRangeDates(selected)));
    } catch (err) {
      setError(err instanceof Error ? err.message : 'No se pudo cargar el reporte.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load(range);
  }, [range, load]);

  return (
    <View style={styles.container}>
      <ReportHeader title="Ventas del día" />
      <QuickRangeChips value={range} onChange={setRange} />

      <ScrollView
        contentContainerStyle={styles.content}
        refreshControl={<RefreshControl refreshing={loading} onRefresh={() => load(range)} />}
      >
        {error ? (
          <Text style={styles.error}>{error}</Text>
        ) : loading && !summary ? (
          <ActivityIndicator color={Brand.orange} style={styles.loader} />
        ) : summary ? (
          <View style={styles.statsRow}>
            <StatCard label="Total vendido" value={formatCurrency(summary.totalSales)} accent />
            <StatCard label="Ventas" value={String(summary.transactionCount)} />
            <StatCard label="Ticket promedio" value={formatCurrency(summary.averageTicket)} />
          </View>
        ) : null}
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: Brand.cream },
  content: { padding: 16, gap: 12 },
  statsRow: { flexDirection: 'row', gap: 10 },
  loader: { marginTop: 40 },
  error: { color: Brand.danger, fontSize: 14, textAlign: 'center', marginTop: 24 },
});

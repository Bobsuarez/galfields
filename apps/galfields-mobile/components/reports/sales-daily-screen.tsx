import { useCallback, useEffect, useState } from 'react';
import { ActivityIndicator, RefreshControl, ScrollView, StyleSheet, Text, View } from 'react-native';
import { ReportHeader } from './report-header';
import { DEFAULT_RANGES, QuickRangeChips } from './quick-range-chips';
import { DateRangePicker } from './date-range-picker';
import { StatCard } from './stat-card';
import { Brand } from '@/constants/theme';
import { formatCurrency } from '@/utils/currency';
import { SimpleDateRange } from '@/utils/date-range';
import { useReportDateRange } from '@/hooks/use-report-date-range';
import { fetchSalesSummary, SalesSummary } from '@/services/reports-api';
import { useThemeColors } from '@/hooks/use-theme-colors';

const RANGES = [...DEFAULT_RANGES, 'custom' as const];

export function SalesDailyScreen() {
  const colors = useThemeColors();
  const { range, setRange, customRange, setCustomRange, dates } = useReportDateRange('today');
  const [summary, setSummary] = useState<SalesSummary | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async (selected: SimpleDateRange) => {
    setLoading(true);
    setError(null);
    try {
      setSummary(await fetchSalesSummary(selected));
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

  return (
    <View style={[styles.container, { backgroundColor: colors.background }]}>
      <ReportHeader title="Ventas del día" />
      <QuickRangeChips value={range} onChange={setRange} ranges={RANGES} />
      {range === 'custom' && <DateRangePicker range={customRange} onChange={setCustomRange} />}

      <ScrollView
        contentContainerStyle={styles.content}
        refreshControl={<RefreshControl refreshing={loading} onRefresh={() => load(dates)} />}
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
  container: { flex: 1 },
  content: { padding: 16, gap: 12 },
  statsRow: { flexDirection: 'row', gap: 10 },
  loader: { marginTop: 40 },
  error: { color: Brand.danger, fontSize: 14, textAlign: 'center', marginTop: 24 },
});

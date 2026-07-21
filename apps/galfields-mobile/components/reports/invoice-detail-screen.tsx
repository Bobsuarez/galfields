import { useEffect, useState } from 'react';
import { ActivityIndicator, ScrollView, StyleSheet, Text, View } from 'react-native';
import { ReportHeader } from './report-header';
import { Brand } from '@/constants/theme';
import { useThemeColors } from '@/hooks/use-theme-colors';
import { formatCurrency } from '@/utils/currency';
import { fetchInvoiceDetail, InvoiceDetail } from '@/services/reports-api';

interface InvoiceDetailScreenProps {
  transactionId: number;
}

export function InvoiceDetailScreen({ transactionId }: InvoiceDetailScreenProps) {
  const colors = useThemeColors();
  const [invoice, setInvoice] = useState<InvoiceDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);
    fetchInvoiceDetail(transactionId)
      .then(result => {
        if (!cancelled) setInvoice(result);
      })
      .catch(err => {
        if (!cancelled) setError(err instanceof Error ? err.message : 'No se pudo cargar la factura.');
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [transactionId]);

  return (
    <View style={[styles.container, { backgroundColor: colors.background }]}>
      <ReportHeader title={`Factura #${transactionId}`} />

      {error ? (
        <Text style={styles.error}>{error}</Text>
      ) : loading || !invoice ? (
        <ActivityIndicator color={Brand.orange} style={styles.loader} />
      ) : (
        <ScrollView contentContainerStyle={styles.content}>
          <View style={styles.summaryCard}>
            <Text style={styles.summaryDate}>
              {new Date(invoice.transactionDate).toLocaleString('es-CO', { dateStyle: 'medium', timeStyle: 'short' })}
            </Text>
            <Text style={styles.summaryEmployee}>Atendido por {invoice.employeeName}</Text>
            <Text style={styles.summaryTotal}>{formatCurrency(invoice.totalAmount)}</Text>
            {invoice.discountAmount > 0 ? (
              <Text style={styles.summaryDiscount}>Descuento: {formatCurrency(invoice.discountAmount)}</Text>
            ) : null}
          </View>

          <Text style={[styles.sectionTitle, { color: colors.textSecondary }]}>Productos</Text>
          <View style={[styles.card, { backgroundColor: colors.card }]}>
            {invoice.items.map((item, index) => (
              <View key={`${item.sku}-${index}`} style={[styles.lineRow, { borderBottomColor: colors.border }]}>
                <View style={styles.lineInfo}>
                  <Text style={[styles.lineName, { color: colors.text }]} numberOfLines={1}>
                    {item.productName}
                  </Text>
                  <Text style={[styles.lineMeta, { color: colors.textSecondary }]}>
                    {item.quantity} × {formatCurrency(item.unitPrice)}
                  </Text>
                </View>
                <Text style={[styles.lineSubtotal, { color: colors.text }]}>{formatCurrency(item.subtotal)}</Text>
              </View>
            ))}
          </View>

          <Text style={[styles.sectionTitle, { color: colors.textSecondary }]}>Pagos</Text>
          <View style={[styles.card, { backgroundColor: colors.card }]}>
            {invoice.payments.map((payment, index) => (
              <View key={`${payment.methodName}-${index}`} style={[styles.lineRow, { borderBottomColor: colors.border }]}>
                <Text style={[styles.lineName, { color: colors.text }]}>{payment.methodName}</Text>
                <Text style={[styles.lineSubtotal, { color: colors.text }]}>{formatCurrency(payment.amount)}</Text>
              </View>
            ))}
          </View>
        </ScrollView>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  loader: { marginTop: 40 },
  error: { color: Brand.danger, fontSize: 14, textAlign: 'center', marginTop: 24 },
  content: { padding: 16, gap: 16 },
  summaryCard: {
    backgroundColor: Brand.orange,
    borderRadius: 14,
    padding: 16,
    gap: 4,
  },
  summaryDate: { fontSize: 13, color: 'rgba(255,255,255,0.85)' },
  summaryEmployee: { fontSize: 13, color: 'rgba(255,255,255,0.85)' },
  summaryTotal: { fontSize: 24, fontWeight: '700', color: '#fff', marginTop: 6 },
  summaryDiscount: { fontSize: 12, color: 'rgba(255,255,255,0.85)' },
  sectionTitle: { fontSize: 13, fontWeight: '700', marginBottom: -8 },
  card: {
    borderRadius: 14,
    overflow: 'hidden',
  },
  lineRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingVertical: 12,
    paddingHorizontal: 16,
    borderBottomWidth: StyleSheet.hairlineWidth,
  },
  lineInfo: { flex: 1, gap: 2, paddingRight: 8 },
  lineName: { fontSize: 14, fontWeight: '600' },
  lineMeta: { fontSize: 12 },
  lineSubtotal: { fontSize: 14, fontWeight: '700' },
});

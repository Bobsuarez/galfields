import { useEffect, useState } from 'react';
import { ActivityIndicator, ScrollView, StyleSheet, Text, View } from 'react-native';
import { ReportHeader } from './report-header';
import { Brand } from '@/constants/theme';
import { formatCurrency } from '@/utils/currency';
import { fetchInvoiceDetail, InvoiceDetail } from '@/services/reports-api';

interface InvoiceDetailScreenProps {
  transactionId: number;
}

export function InvoiceDetailScreen({ transactionId }: InvoiceDetailScreenProps) {
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
    <View style={styles.container}>
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

          <Text style={styles.sectionTitle}>Productos</Text>
          <View style={styles.card}>
            {invoice.items.map((item, index) => (
              <View key={`${item.sku}-${index}`} style={styles.lineRow}>
                <View style={styles.lineInfo}>
                  <Text style={styles.lineName} numberOfLines={1}>
                    {item.productName}
                  </Text>
                  <Text style={styles.lineMeta}>
                    {item.quantity} × {formatCurrency(item.unitPrice)}
                  </Text>
                </View>
                <Text style={styles.lineSubtotal}>{formatCurrency(item.subtotal)}</Text>
              </View>
            ))}
          </View>

          <Text style={styles.sectionTitle}>Pagos</Text>
          <View style={styles.card}>
            {invoice.payments.map((payment, index) => (
              <View key={`${payment.methodName}-${index}`} style={styles.lineRow}>
                <Text style={styles.lineName}>{payment.methodName}</Text>
                <Text style={styles.lineSubtotal}>{formatCurrency(payment.amount)}</Text>
              </View>
            ))}
          </View>
        </ScrollView>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: Brand.cream },
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
  sectionTitle: { fontSize: 13, fontWeight: '700', color: '#8A7060', marginBottom: -8 },
  card: {
    backgroundColor: '#fff',
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
    borderBottomColor: '#EEE8E0',
  },
  lineInfo: { flex: 1, gap: 2, paddingRight: 8 },
  lineName: { fontSize: 14, fontWeight: '600', color: '#1A1A1A' },
  lineMeta: { fontSize: 12, color: '#8A7060' },
  lineSubtotal: { fontSize: 14, fontWeight: '700', color: '#1A1A1A' },
});

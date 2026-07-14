import { Pressable, StyleSheet, Text, View } from 'react-native';
import { IconSymbol } from '@/components/ui/icon-symbol';
import { Brand } from '@/constants/theme';
import { formatCurrency } from '@/utils/currency';
import type { InvoiceSummary } from '@/services/reports-api';

interface InvoiceListItemProps {
  invoice: InvoiceSummary;
  onPress: () => void;
}

export function InvoiceListItem({ invoice, onPress }: InvoiceListItemProps) {
  const date = new Date(invoice.transactionDate);

  return (
    <Pressable onPress={onPress} style={({ pressed }) => [styles.container, pressed && styles.pressed]}>
      <View style={styles.info}>
        <Text style={styles.title}>Factura #{invoice.transactionId}</Text>
        <Text style={styles.meta}>
          {date.toLocaleString('es-CO', { dateStyle: 'medium', timeStyle: 'short' })} · {invoice.employeeName} ·{' '}
          {invoice.itemCount} ítem{invoice.itemCount === 1 ? '' : 's'}
        </Text>
      </View>
      <Text style={styles.amount}>{formatCurrency(invoice.totalAmount)}</Text>
      <IconSymbol name="chevron.right" size={18} color="#CCC" />
    </Pressable>
  );
}

const styles = StyleSheet.create({
  container: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#fff',
    paddingVertical: 14,
    paddingHorizontal: 16,
    gap: 10,
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: '#EEE8E0',
  },
  pressed: { opacity: 0.7 },
  info: { flex: 1, gap: 2 },
  title: { fontSize: 14, fontWeight: '600', color: '#1A1A1A' },
  meta: { fontSize: 11, color: '#8A7060' },
  amount: { fontSize: 15, fontWeight: '700', color: Brand.orange },
});

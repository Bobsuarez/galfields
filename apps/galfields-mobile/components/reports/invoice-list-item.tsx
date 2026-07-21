import { Pressable, StyleSheet, Text, View } from 'react-native';
import { IconSymbol } from '@/components/ui/icon-symbol';
import { Brand } from '@/constants/theme';
import { useThemeColors } from '@/hooks/use-theme-colors';
import { formatCurrency } from '@/utils/currency';
import type { InvoiceSummary } from '@/services/reports-api';

interface InvoiceListItemProps {
  invoice: InvoiceSummary;
  onPress: () => void;
}

export function InvoiceListItem({ invoice, onPress }: InvoiceListItemProps) {
  const colors = useThemeColors();
  const date = new Date(invoice.transactionDate);

  return (
    <Pressable
      onPress={onPress}
      style={({ pressed }) => [
        styles.container,
        { backgroundColor: colors.card, borderBottomColor: colors.border },
        pressed && styles.pressed,
      ]}
    >
      <View style={styles.info}>
        <Text style={[styles.title, { color: colors.text }]}>Factura #{invoice.transactionId}</Text>
        <Text style={[styles.meta, { color: colors.textSecondary }]}>
          {date.toLocaleString('es-CO', { dateStyle: 'medium', timeStyle: 'short' })} · {invoice.employeeName} ·{' '}
          {invoice.itemCount} ítem{invoice.itemCount === 1 ? '' : 's'}
        </Text>
      </View>
      <Text style={styles.amount}>{formatCurrency(invoice.totalAmount)}</Text>
      <IconSymbol name="chevron.right" size={18} color={colors.icon} />
    </Pressable>
  );
}

const styles = StyleSheet.create({
  container: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 14,
    paddingHorizontal: 16,
    gap: 10,
    borderBottomWidth: StyleSheet.hairlineWidth,
  },
  pressed: { opacity: 0.7 },
  info: { flex: 1, gap: 2 },
  title: { fontSize: 14, fontWeight: '600' },
  meta: { fontSize: 11 },
  amount: { fontSize: 15, fontWeight: '700', color: Brand.orange },
});

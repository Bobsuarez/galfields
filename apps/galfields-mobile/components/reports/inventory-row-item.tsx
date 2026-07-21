import { StyleSheet, Text, View } from 'react-native';
import { Brand } from '@/constants/theme';
import { useThemeColors } from '@/hooks/use-theme-colors';
import type { InventoryRow } from '@/services/reports-api';

interface InventoryRowItemProps {
  row: InventoryRow;
  /** Highlights the quantity in red when true (used by the low-stock screen). */
  warn?: boolean;
}

export function InventoryRowItem({ row, warn }: InventoryRowItemProps) {
  const colors = useThemeColors();
  return (
    <View style={[styles.container, { backgroundColor: colors.card, borderBottomColor: colors.border }]}>
      <View style={styles.info}>
        <Text style={[styles.name, { color: colors.text }]} numberOfLines={1}>
          {row.productName}
        </Text>
        <Text style={[styles.meta, { color: colors.textSecondary }]} numberOfLines={1}>
          {row.sku} · {row.categoryName ?? 'Sin categoría'} · {row.locationName}
        </Text>
      </View>
      <Text style={[styles.qty, warn && styles.qtyWarn]}>{row.quantityOnHand}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 12,
    paddingHorizontal: 16,
    gap: 12,
    borderBottomWidth: StyleSheet.hairlineWidth,
  },
  info: { flex: 1, gap: 2 },
  name: { fontSize: 14, fontWeight: '600' },
  meta: { fontSize: 11 },
  qty: { fontSize: 16, fontWeight: '700', color: Brand.success },
  qtyWarn: { color: Brand.danger },
});

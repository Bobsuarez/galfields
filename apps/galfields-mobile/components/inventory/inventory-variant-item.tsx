import { Image, Pressable, StyleSheet, Text, View } from 'react-native';
import { IconSymbol } from '@/components/ui/icon-symbol';
import { Brand } from '@/constants/theme';
import { useThemeColors } from '@/hooks/use-theme-colors';

export interface InventoryVariantRow {
  productId: number;
  variantId: number;
  displayName: string;
  sku: string;
  barcode: string;
  stock: number;
  active: boolean;
  imageUrl: string | null;
}

interface InventoryVariantItemProps {
  row: InventoryVariantRow;
  onPressStock: () => void;
  onToggleActive: () => void;
}

export function InventoryVariantItem({ row, onPressStock, onToggleActive }: InventoryVariantItemProps) {
  const colors = useThemeColors();
  return (
    <View
      style={[
        styles.container,
        { backgroundColor: colors.card, borderBottomColor: colors.border },
        !row.active && styles.containerInactive,
      ]}
    >
      <View style={[styles.thumb, { backgroundColor: colors.border }]}>
        {row.imageUrl ? (
          <Image source={{ uri: row.imageUrl }} style={styles.thumbImage} resizeMode="cover" />
        ) : (
          <IconSymbol name="shippingbox.fill" size={20} color={colors.placeholder} />
        )}
      </View>

      <View style={styles.info}>
        <Text style={[styles.name, { color: colors.text }]} numberOfLines={1}>
          {row.displayName}
        </Text>
        <Text style={[styles.meta, { color: colors.textSecondary }]} numberOfLines={1}>
          {row.sku} · {row.barcode}
        </Text>
        {!row.active ? (
          <View style={styles.badge}>
            <Text style={styles.badgeText}>Desactivado</Text>
          </View>
        ) : null}
      </View>

      <Pressable onPress={onPressStock} style={styles.stockBtn} hitSlop={8}>
        <Text style={[styles.stockValue, row.stock <= 5 && styles.stockLow]}>{row.stock}</Text>
        <Text style={[styles.stockLabel, { color: colors.textSecondary }]}>en stock</Text>
      </Pressable>

      <Pressable onPress={onToggleActive} style={styles.actionBtn} hitSlop={8}>
        <IconSymbol
          name={row.active ? 'xmark' : 'checkmark.circle.fill'}
          size={20}
          color={row.active ? Brand.danger : Brand.success}
        />
      </Pressable>
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
  containerInactive: { opacity: 0.55 },
  thumb: {
    width: 40,
    height: 40,
    borderRadius: 10,
    alignItems: 'center',
    justifyContent: 'center',
    overflow: 'hidden',
  },
  thumbImage: { width: '100%', height: '100%' },
  info: { flex: 1, gap: 2 },
  name: { fontSize: 14, fontWeight: '600' },
  meta: { fontSize: 11 },
  badge: {
    alignSelf: 'flex-start',
    marginTop: 2,
    borderRadius: 8,
    paddingHorizontal: 8,
    paddingVertical: 2,
    backgroundColor: `${Brand.danger}14`,
  },
  badgeText: { fontSize: 10, fontWeight: '700', color: Brand.danger },
  stockBtn: { alignItems: 'center', paddingHorizontal: 4, minWidth: 52 },
  stockValue: { fontSize: 17, fontWeight: '700', color: Brand.success },
  stockLow: { color: Brand.danger },
  stockLabel: { fontSize: 9 },
  actionBtn: { padding: 6 },
});

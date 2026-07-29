import { Pressable, StyleSheet, Text, View } from 'react-native';
import { Brand } from '@/constants/theme';
import { useThemeColors } from '@/hooks/use-theme-colors';
import { IconSymbol } from '@/components/ui/icon-symbol';

interface ProductUnitListItemProps {
  unitName: string;
  conversionFactor: number;
  unitPrice: number;
  barcode: string;
  isBase: boolean;
  active: boolean;
  onEdit: () => void;
  onDeactivate: () => void;
}

export function ProductUnitListItem({
  unitName,
  conversionFactor,
  unitPrice,
  barcode,
  isBase,
  active,
  onEdit,
  onDeactivate,
}: ProductUnitListItemProps) {
  const colors = useThemeColors();
  return (
    <View style={[styles.container, { backgroundColor: colors.card, borderBottomColor: colors.border }]}>
      <View style={[styles.thumb, { backgroundColor: colors.border }]}>
        <IconSymbol name="square.stack.3d.up.fill" size={20} color={colors.placeholder} />
      </View>
      <View style={styles.info}>
        <Text style={[styles.title, { color: colors.text }]} numberOfLines={1}>
          {unitName}
        </Text>
        <Text style={[styles.subtitle, { color: colors.textSecondary }]} numberOfLines={1}>
          {`× ${conversionFactor} · $${unitPrice.toLocaleString('es-CO')}`}
          {barcode ? ` · ${barcode}` : ''}
        </Text>
        <View style={styles.badgeRow}>
          {isBase ? (
            <View style={[styles.badge, styles.badgeBase]}>
              <Text style={[styles.badgeText, styles.badgeTextBase]}>Base</Text>
            </View>
          ) : null}
          <View style={[styles.badge, active ? styles.badgeActive : styles.badgeInactive]}>
            <Text style={[styles.badgeText, active ? styles.badgeTextActive : styles.badgeTextInactive]}>
              {active ? 'Activa' : 'Inactiva'}
            </Text>
          </View>
        </View>
      </View>
      <View style={styles.actions}>
        <Pressable onPress={onEdit} hitSlop={8} style={styles.actionBtn}>
          <IconSymbol name="pencil" size={20} color={Brand.orange} />
        </Pressable>
        {/* The base unit can't be deactivated - backend 409s if attempted
         * (see backend/pos's CLAUDE.md, "CRUD .../units") - hide the action
         * instead of letting the user hit that error. */}
        {!isBase && (
          <Pressable onPress={onDeactivate} hitSlop={8} style={styles.actionBtn}>
            <IconSymbol name="trash.fill" size={20} color={Brand.danger} />
          </Pressable>
        )}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 14,
    paddingHorizontal: 16,
    gap: 12,
    borderBottomWidth: StyleSheet.hairlineWidth,
  },
  thumb: {
    width: 44,
    height: 44,
    borderRadius: 12,
    alignItems: 'center',
    justifyContent: 'center',
  },
  info: { flex: 1, gap: 4 },
  title: { fontSize: 15, fontWeight: '600' },
  subtitle: { fontSize: 13 },
  badgeRow: { flexDirection: 'row', gap: 6 },
  badge: { alignSelf: 'flex-start', borderRadius: 8, paddingHorizontal: 8, paddingVertical: 2 },
  badgeBase: { backgroundColor: `${Brand.orange}1A` },
  badgeActive: { backgroundColor: `${Brand.success}1A` },
  badgeInactive: { backgroundColor: `${Brand.danger}14` },
  badgeText: { fontSize: 11, fontWeight: '600' },
  badgeTextBase: { color: Brand.orange },
  badgeTextActive: { color: Brand.success },
  badgeTextInactive: { color: Brand.danger },
  actions: { flexDirection: 'row', gap: 4 },
  actionBtn: { padding: 6 },
});

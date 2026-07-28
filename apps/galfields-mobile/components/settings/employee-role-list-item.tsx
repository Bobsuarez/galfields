import { Pressable, StyleSheet, Text, View } from 'react-native';
import { Brand } from '@/constants/theme';
import { useThemeColors } from '@/hooks/use-theme-colors';
import { IconSymbol } from '@/components/ui/icon-symbol';
import { PERMISSION_MODULES } from '@/services/employee-roles-api';

interface EmployeeRoleListItemProps {
  roleName: string;
  permissions: Record<string, boolean>;
  canLoginMobile: boolean;
  canLoginDesktop: boolean;
  onEdit: () => void;
  onDelete: () => void;
}

export function EmployeeRoleListItem({
  roleName,
  permissions,
  canLoginMobile,
  canLoginDesktop,
  onEdit,
  onDelete,
}: EmployeeRoleListItemProps) {
  const colors = useThemeColors();
  const enabledModules = PERMISSION_MODULES.filter(m => permissions[m.key]).map(m => m.label);

  return (
    <View style={[styles.container, { backgroundColor: colors.card, borderBottomColor: colors.border }]}>
      <View style={[styles.thumb, { backgroundColor: colors.border }]}>
        <IconSymbol name="shield.fill" size={20} color={colors.placeholder} />
      </View>
      <View style={styles.info}>
        <Text style={[styles.title, { color: colors.text }]} numberOfLines={1}>
          {roleName}
        </Text>
        <Text style={[styles.subtitle, { color: colors.textSecondary }]} numberOfLines={1}>
          {enabledModules.length > 0 ? enabledModules.join(', ') : 'Sin permisos'}
        </Text>
        <View style={styles.badgeRow}>
          {canLoginMobile ? (
            <View style={[styles.badge, styles.badgeMobile]}>
              <Text style={[styles.badgeText, styles.badgeTextMobile]}>Móvil</Text>
            </View>
          ) : null}
          {canLoginDesktop ? (
            <View style={[styles.badge, styles.badgeDesktop]}>
              <Text style={[styles.badgeText, styles.badgeTextDesktop]}>Escritorio</Text>
            </View>
          ) : null}
        </View>
      </View>
      <View style={styles.actions}>
        <Pressable onPress={onEdit} hitSlop={8} style={styles.actionBtn}>
          <IconSymbol name="pencil" size={20} color={Brand.orange} />
        </Pressable>
        <Pressable onPress={onDelete} hitSlop={8} style={styles.actionBtn}>
          <IconSymbol name="trash.fill" size={20} color={Brand.danger} />
        </Pressable>
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
  badgeRow: { flexDirection: 'row', gap: 6, marginTop: 2 },
  badge: { borderRadius: 8, paddingHorizontal: 8, paddingVertical: 2 },
  badgeMobile: { backgroundColor: `${Brand.orange}1A` },
  badgeDesktop: { backgroundColor: `${Brand.success}1A` },
  badgeText: { fontSize: 11, fontWeight: '600' },
  badgeTextMobile: { color: Brand.orange },
  badgeTextDesktop: { color: Brand.success },
  actions: { flexDirection: 'row', gap: 4 },
  actionBtn: { padding: 6 },
});

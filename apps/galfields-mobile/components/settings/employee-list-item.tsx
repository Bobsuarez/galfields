import { Pressable, StyleSheet, Text, View } from 'react-native';
import { Brand } from '@/constants/theme';
import { useThemeColors } from '@/hooks/use-theme-colors';
import { IconSymbol } from '@/components/ui/icon-symbol';

interface EmployeeListItemProps {
  fullName: string;
  username: string;
  roleName: string;
  active: boolean;
  onEdit: () => void;
  onDelete: () => void;
}

export function EmployeeListItem({ fullName, username, roleName, active, onEdit, onDelete }: EmployeeListItemProps) {
  const colors = useThemeColors();
  return (
    <View style={[styles.container, { backgroundColor: colors.card, borderBottomColor: colors.border }]}>
      <View style={[styles.thumb, { backgroundColor: colors.border }]}>
        <Text style={styles.avatarInitial}>{fullName.charAt(0).toUpperCase() || '?'}</Text>
      </View>
      <View style={styles.info}>
        <Text style={[styles.title, { color: colors.text }]} numberOfLines={1}>
          {fullName}
        </Text>
        <Text style={[styles.subtitle, { color: colors.textSecondary }]} numberOfLines={1}>
          @{username} · {roleName}
        </Text>
        <View style={[styles.badge, active ? styles.badgeActive : styles.badgeInactive]}>
          <Text style={[styles.badgeText, active ? styles.badgeTextActive : styles.badgeTextInactive]}>
            {active ? 'Activo' : 'Inactivo'}
          </Text>
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
    borderRadius: 22,
    alignItems: 'center',
    justifyContent: 'center',
  },
  avatarInitial: { fontSize: 16, fontWeight: '700', color: Brand.brown },
  info: { flex: 1, gap: 4 },
  title: { fontSize: 15, fontWeight: '600' },
  subtitle: { fontSize: 13 },
  badge: { alignSelf: 'flex-start', borderRadius: 8, paddingHorizontal: 8, paddingVertical: 2 },
  badgeActive: { backgroundColor: `${Brand.success}1A` },
  badgeInactive: { backgroundColor: `${Brand.danger}14` },
  badgeText: { fontSize: 11, fontWeight: '600' },
  badgeTextActive: { color: Brand.success },
  badgeTextInactive: { color: Brand.danger },
  actions: { flexDirection: 'row', gap: 4 },
  actionBtn: { padding: 6 },
});

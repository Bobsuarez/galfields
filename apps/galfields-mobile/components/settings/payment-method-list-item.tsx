import { Image, Pressable, StyleSheet, Text, View } from 'react-native';
import { Brand } from '@/constants/theme';
import { IconSymbol } from '@/components/ui/icon-symbol';

interface PaymentMethodListItemProps {
  name: string;
  active: boolean;
  imageUrl: string | null;
  onEdit: () => void;
  onDelete: () => void;
}

export function PaymentMethodListItem({ name, active, imageUrl, onEdit, onDelete }: PaymentMethodListItemProps) {
  return (
    <View style={styles.container}>
      <View style={styles.thumb}>
        {imageUrl ? (
          <Image source={{ uri: imageUrl }} style={styles.thumbImage} resizeMode="contain" />
        ) : (
          <IconSymbol name="creditcard.fill" size={22} color="#B0A090" />
        )}
      </View>
      <View style={styles.info}>
        <Text style={styles.title} numberOfLines={1}>
          {name}
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
    backgroundColor: '#fff',
    paddingVertical: 14,
    paddingHorizontal: 16,
    gap: 12,
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: '#EEE8E0',
  },
  thumb: {
    width: 44,
    height: 44,
    borderRadius: 12,
    backgroundColor: '#F7F1E8',
    alignItems: 'center',
    justifyContent: 'center',
    overflow: 'hidden',
  },
  thumbImage: { width: '100%', height: '100%' },
  info: { flex: 1, gap: 4 },
  title: { fontSize: 15, fontWeight: '600', color: '#1A1A1A' },
  badge: { alignSelf: 'flex-start', borderRadius: 8, paddingHorizontal: 8, paddingVertical: 2 },
  badgeActive: { backgroundColor: `${Brand.success}1A` },
  badgeInactive: { backgroundColor: `${Brand.danger}14` },
  badgeText: { fontSize: 11, fontWeight: '600' },
  badgeTextActive: { color: Brand.success },
  badgeTextInactive: { color: Brand.danger },
  actions: { flexDirection: 'row', gap: 4 },
  actionBtn: { padding: 6 },
});

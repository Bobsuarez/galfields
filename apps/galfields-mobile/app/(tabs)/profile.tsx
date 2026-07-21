import { Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { router } from 'expo-router';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { IconSymbol } from '@/components/ui/icon-symbol';
import { useAuth } from '@/contexts/auth-context';
import { Brand } from '@/constants/theme';
import { useThemeColors } from '@/hooks/use-theme-colors';

interface ProfileItem {
  label: string;
  icon: string;
  href?: string;
  danger?: boolean;
}

const PROFILE_ITEMS: ProfileItem[] = [
  { label: 'Inicio', icon: 'house.fill', href: '/(tabs)' },
  { label: 'Productos', icon: 'shippingbox.fill', href: '/products' },
  { label: 'Ventas', icon: 'chart.bar.fill' },
  { label: 'Inventario', icon: 'tray.fill' },
  { label: 'Carrito', icon: 'cart.fill' },
  { label: 'Historial', icon: 'clock.fill' },
  { label: 'Configuración', icon: 'gearshape.fill' },
];

export default function ProfileScreen() {
  const { user, logout } = useAuth();
  const insets = useSafeAreaInsets();
  const colors = useThemeColors();

  const handleItemPress = (item: ProfileItem) => {
    if (item.href) router.push(item.href as any);
  };

  return (
    <View style={[styles.container, { paddingTop: 0, backgroundColor: colors.background }]}>
      {/* Header */}
      <View style={[styles.header, { paddingTop: insets.top + 20 }]}>
        <View style={styles.avatar}>
          <Text style={styles.avatarInitial}>{(user?.username ?? 'C')[0]}</Text>
        </View>
        <Text style={styles.name}>{user?.username ?? 'Cajero'}</Text>
        <Text style={styles.role}>{user?.role ?? 'Administrador'}</Text>
      </View>

      <ScrollView showsVerticalScrollIndicator={false}>
        {PROFILE_ITEMS.map(item => (
          <Pressable
            key={item.label}
            onPress={() => handleItemPress(item)}
            style={({ pressed }) => [styles.item, { borderBottomColor: colors.border }, pressed && styles.itemPressed]}
          >
            <IconSymbol name={item.icon as any} size={22} color={Brand.orange} />
            <Text style={[styles.itemLabel, { color: colors.text }]}>{item.label}</Text>
            <IconSymbol name="chevron.right" size={18} color={colors.icon} />
          </Pressable>
        ))}

        <View style={[styles.separator, { backgroundColor: colors.background }]} />

        <Pressable
          onPress={logout}
          style={({ pressed }) => [styles.item, { borderBottomColor: colors.border }, pressed && styles.itemPressed]}
        >
          <IconSymbol name="arrow.right.square" size={22} color={Brand.danger} />
          <Text style={[styles.itemLabel, styles.logoutLabel]}>Cerrar sesión</Text>
        </Pressable>
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  header: {
    backgroundColor: Brand.brown,
    paddingBottom: 28,
    paddingHorizontal: 20,
    alignItems: 'flex-start',
  },
  avatar: {
    width: 64,
    height: 64,
    borderRadius: 32,
    backgroundColor: Brand.orange,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 12,
  },
  avatarInitial: { fontSize: 28, fontWeight: '700', color: '#fff' },
  name: { fontSize: 20, fontWeight: '700', color: '#fff' },
  role: { fontSize: 13, color: 'rgba(255,255,255,0.65)', marginTop: 2 },
  item: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 16,
    paddingHorizontal: 20,
    gap: 16,
    borderBottomWidth: StyleSheet.hairlineWidth,
  },
  itemPressed: { backgroundColor: `${Brand.orange}08` },
  itemLabel: { flex: 1, fontSize: 15, fontWeight: '500' },
  separator: { height: 8 },
  logoutLabel: { color: Brand.danger },
});

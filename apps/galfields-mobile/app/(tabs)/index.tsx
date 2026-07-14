import { useState } from 'react';
import { Image, Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { router } from 'expo-router';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { MenuCard } from '@/components/home/menu-card';
import { SideDrawer } from '@/components/layout/side-drawer';
import { IconSymbol } from '@/components/ui/icon-symbol';
import { useAuth } from '@/contexts/auth-context';
import { Brand, Colors } from '@/constants/theme';
import { useColorScheme } from '@/hooks/use-color-scheme';

const MENU_ITEMS = [
  {
    icon: 'square.grid.2x2.fill',
    label: 'Productos',
    subtitle: 'Ver y gestionar\nproductos',
    href: '/products' as const,
  },
  {
    icon: 'chart.bar.fill',
    label: 'Ventas',
    subtitle: 'Registrar\nventas',
    href: null,
  },
  {
    icon: 'tray.fill',
    label: 'Inventario',
    subtitle: 'Ver stock\nactual',
    href: '/inventory' as const,
  },
  {
    icon: 'cart.fill',
    label: 'Carrito',
    subtitle: 'Ver carrito\nde compras',
    href: null,
  },
  {
    icon: 'clock.fill',
    label: 'Historial',
    subtitle: 'Ventas y\nmovimientos',
    href: null,
  },
  {
    icon: 'chart.pie.fill',
    label: 'Reportes',
    subtitle: 'Ventas, caja\ne inventario',
    href: '/reports' as const,
  },
  {
    icon: 'gearshape.fill',
    label: 'Configuración',
    subtitle: 'Ajustes de la\naplicación',
    href: '/settings' as const,
  },
] as const;

export default function HomeScreen() {
  const { user } = useAuth();
  const [drawerOpen, setDrawerOpen] = useState(false);
  const insets = useSafeAreaInsets();
  const scheme = useColorScheme() ?? 'light';
  const colors = Colors[scheme];

  return (
    <View style={[styles.container, { backgroundColor: colors.background }]}>
      {/* Header */}
      <View style={[styles.header, { paddingTop: insets.top + 8 }]}>
        <Image
          source={require('@/assets/images/icon.png')}
          style={styles.headerLogo}
          resizeMode="contain"
        />
        <View style={styles.brandRow}>
          <Text style={styles.brandGar}>Gar</Text>
          <Text style={styles.brandPOS}>POS</Text>
        </View>
        <Text style={styles.headerSubtitle}>PUNTO DE VENTA</Text>
        <View style={styles.headerActions}>
          <Pressable hitSlop={10}>
            <IconSymbol name="bell" size={24} color="#fff" />
          </Pressable>
          <Pressable onPress={() => setDrawerOpen(true)} hitSlop={10}>
            <IconSymbol name="line.3.horizontal" size={24} color="#fff" />
          </Pressable>
        </View>
      </View>

      <ScrollView contentContainerStyle={styles.content} showsVerticalScrollIndicator={false}>
        {/* Greeting */}
        <View style={styles.greeting}>
          <Text style={[styles.greetingTop, { color: Brand.orange }]}>
            ¡Hola, {user?.username ?? 'Cajero'}!
          </Text>
          <Text style={[styles.greetingBottom, { color: colors.text }]}>
            ¿Qué deseas hacer hoy?
          </Text>
        </View>

        {/* Menu grid */}
        <View style={styles.grid}>
          {MENU_ITEMS.map(item => (
            <View key={item.label} style={styles.gridCell}>
              <MenuCard
                icon={item.icon}
                label={item.label}
                subtitle={item.subtitle}
                onPress={item.href ? () => router.push(item.href as any) : undefined}
              />
            </View>
          ))}
        </View>
      </ScrollView>

      <SideDrawer
        visible={drawerOpen}
        onClose={() => setDrawerOpen(false)}
        activeRoute="/(tabs)"
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 16,
    paddingBottom: 16,
    backgroundColor: Brand.brown,
    gap: 6,
  },
  headerLogo: { width: 34, height: 34 },
  brandRow: { flexDirection: 'row', alignItems: 'baseline' },
  brandGar: { fontSize: 20, fontWeight: '800', color: '#fff' },
  brandPOS: { fontSize: 20, fontWeight: '800', color: Brand.orange },
  headerSubtitle: { fontSize: 9, color: 'rgba(255,255,255,0.6)', letterSpacing: 1.5, marginTop: 2 },
  headerActions: { flex: 1, flexDirection: 'row', justifyContent: 'flex-end', gap: 16 },
  content: { padding: 16, paddingBottom: 32 },
  greeting: { marginBottom: 24, marginTop: 8 },
  greetingTop: { fontSize: 22, fontWeight: '700' },
  greetingBottom: { fontSize: 16, marginTop: 2 },
  grid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 12,
  },
  gridCell: { width: '47.5%' },
});

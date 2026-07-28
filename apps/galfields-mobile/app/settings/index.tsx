import { Pressable, StyleSheet, Text, View } from 'react-native';
import { router } from 'expo-router';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { SettingsMenuRow } from '@/components/settings/settings-menu-row';
import { IconSymbol } from '@/components/ui/icon-symbol';
import { Brand } from '@/constants/theme';
import { useThemeColors } from '@/hooks/use-theme-colors';

const SETTINGS_ITEMS = [
  {
    icon: 'tag.fill',
    label: 'Categorías',
    subtitle: 'Gestionar categorías de productos',
    href: '/settings/categories' as const,
  },
  {
    icon: 'building.2.fill',
    label: 'Marcas',
    subtitle: 'Gestionar marcas de productos',
    href: '/settings/brands' as const,
  },
  {
    icon: 'mappin.and.ellipse',
    label: 'Ubicaciones',
    subtitle: 'Gestionar bodegas y puntos de venta',
    href: '/settings/locations' as const,
  },
  {
    icon: 'creditcard.fill',
    label: 'Métodos de pago',
    subtitle: 'Gestionar métodos de pago disponibles',
    href: '/settings/payment-methods' as const,
  },
  {
    icon: 'desktopcomputer',
    label: 'Terminales',
    subtitle: 'Gestionar terminales de venta (galfield-pos)',
    href: '/settings/terminals' as const,
  },
  {
    icon: 'shield.fill',
    label: 'Roles',
    subtitle: 'Permisos por módulo y acceso a mobile/desktop',
    href: '/settings/roles' as const,
  },
  {
    icon: 'person.fill',
    label: 'Empleados',
    subtitle: 'Usuario, clave, rol y terminales asignadas',
    href: '/settings/employees' as const,
  },
  {
    icon: 'doc.text.fill',
    label: 'Numeración de facturas',
    subtitle: 'Prefijo y rango DIAN autorizado por terminal',
    href: '/settings/invoicing' as const,
  },
  {
    icon: 'server.rack',
    label: 'Servidor',
    subtitle: 'URL del backend al que se conecta la app',
    href: '/settings/server' as const,
  },
  {
    icon: 'lock.fill',
    label: 'Acceso a Reportes',
    subtitle: 'Generar código de seguridad para autorizar reportes en el POS',
    href: '/settings/reports-security' as const,
  },
  {
    icon: 'magnifyingglass',
    label: 'Búsqueda de imágenes',
    subtitle: 'Proveedor usado para buscar fotos de producto',
    href: '/settings/image-search-provider' as const,
  },
] as const;

export default function SettingsScreen() {
  const insets = useSafeAreaInsets();
  const colors = useThemeColors();

  return (
    <View style={[styles.container, { backgroundColor: colors.background }]}>
      <View style={[styles.header, { paddingTop: insets.top + 8 }]}>
        <Pressable onPress={() => router.back()} hitSlop={10}>
          <IconSymbol name="arrow.left" size={24} color="#fff" />
        </Pressable>
        <Text style={styles.headerTitle}>Configuración</Text>
        <View style={styles.headerSpacer} />
      </View>

      <View style={styles.content}>
        <View style={styles.card}>
          {SETTINGS_ITEMS.map(item => (
            <SettingsMenuRow
              key={item.label}
              icon={item.icon}
              label={item.label}
              subtitle={item.subtitle}
              onPress={() => router.push(item.href)}
            />
          ))}
        </View>
      </View>
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
    gap: 12,
  },
  headerTitle: {
    flex: 1,
    fontSize: 20,
    fontWeight: '700',
    color: '#fff',
    textAlign: 'center',
  },
  headerSpacer: { width: 24 },
  content: { padding: 16 },
  card: {
    borderRadius: 16,
    overflow: 'hidden',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.06,
    shadowRadius: 8,
    elevation: 3,
  },
});

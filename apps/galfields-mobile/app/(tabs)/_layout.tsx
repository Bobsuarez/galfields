import { Pressable, StyleSheet } from 'react-native';
import { Redirect, Tabs, router, type Href } from 'expo-router';
import { type BottomTabBarButtonProps } from '@react-navigation/bottom-tabs';
import { PlatformPressable } from '@react-navigation/elements';
import * as Haptics from 'expo-haptics';
import { HapticTab } from '@/components/haptic-tab';
import { IconSymbol } from '@/components/ui/icon-symbol';
import { useAuth } from '@/contexts/auth-context';
import { Brand, Colors } from '@/constants/theme';
import { useColorScheme } from '@/hooks/use-color-scheme';

// Elevated center button for the sale/scan tab — same haptic + navigation
// behavior as a normal tab button (forwards to the real onPress from
// react-navigation), just styled as a raised FAB.
function ScanFab({ ref: _ref, onPress, ...rest }: BottomTabBarButtonProps) {
  const handlePress: NonNullable<BottomTabBarButtonProps['onPress']> = e => {
    if (process.env.EXPO_OS === 'ios') {
      Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Medium);
    }
    onPress?.(e);
  };

  return (
    <Pressable {...rest} onPress={handlePress} style={styles.scanFab} accessibilityRole="button">
      <IconSymbol name="barcode.viewfinder" size={28} color="#fff" />
    </Pressable>
  );
}

/**
 * "products" and "settings" are thin `<Redirect>` files whose only job is to
 * give those routes a tab bar entry — the real screens live in their own
 * top-level stacks (app/products, app/settings), outside the (tabs) group.
 * If we let a normal tab press focus that route, expo-router's <Redirect>
 * fires `router.replace()`, which replaces the whole "(tabs)" entry in the
 * root stack's history with "/products" — leaving no previous screen to go
 * back to, so the back button crashes the navigator. Pushing directly here
 * bypasses the in-tab redirect entirely and keeps normal back-navigation.
 */
function externalTabButton(href: Href) {
  return function ExternalTabButton(props: BottomTabBarButtonProps) {
    return (
      <PlatformPressable
        {...props}
        onPress={() => {
          if (process.env.EXPO_OS === 'ios') {
            Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Light);
          }
          router.push(href);
        }}
      />
    );
  };
}

export default function TabLayout() {
  const { isAuthenticated } = useAuth();
  const scheme = useColorScheme() ?? 'light';
  const colors = Colors[scheme];

  // Guard: redirect to login if not authenticated.
  // <Redirect> runs in the render phase (not an effect), so the Stack is already
  // mounted and ready when this navigation occurs — no "before mounting" error.
  if (!isAuthenticated) {
    return <Redirect href="/login" />;
  }

  return (
    <Tabs
      screenOptions={{
        headerShown: false,
        tabBarShowLabel: false,
        tabBarActiveTintColor: Brand.orange,
        tabBarInactiveTintColor: colors.tabIconDefault,
        tabBarButton: HapticTab,
        tabBarStyle: {
          backgroundColor: colors.card,
          borderTopWidth: 0,
          height: 75,
          paddingBottom: 10,
          shadowColor: '#000',
          shadowOffset: { width: 0, height: -4 },
          shadowOpacity: 0.08,
          shadowRadius: 12,
          elevation: 20,
        },
      }}
    >
      <Tabs.Screen
        name="reports"
        options={{
          title: 'Reportes',
          tabBarIcon: ({ color }) => (
            <IconSymbol name="chart.bar.fill" size={26} color={color} />
          ),
        }}
      />
      <Tabs.Screen
        name="products"
        options={{
          title: 'Productos',
          tabBarIcon: ({ color }) => (
            <IconSymbol name="shippingbox.fill" size={26} color={color} />
          ),
          tabBarButton: externalTabButton('/products'),
        }}
      />
      <Tabs.Screen
        name="scan"
        options={{
          title: 'Registrar venta',
          tabBarButton: ScanFab,
        }}
      />
      <Tabs.Screen
        name="inventory"
        options={{
          title: 'Inventario',
          tabBarIcon: ({ color }) => (
            <IconSymbol name="tray.fill" size={26} color={color} />
          ),
        }}
      />
      <Tabs.Screen
        name="settings"
        options={{
          title: 'Configuración',
          tabBarIcon: ({ color }) => (
            <IconSymbol name="gearshape.fill" size={26} color={color} />
          ),
          tabBarButton: externalTabButton('/settings'),
        }}
      />

      {/* Kept mounted but hidden from the tab bar. "index" stays the
          default screen for the (tabs) group (see unstable_settings.anchor
          in the root layout); "profile" and "explore" are no longer linked
          from anywhere in the UI now that the footer dropped Inicio/Perfil. */}
      <Tabs.Screen name="index" options={{ href: null }} />
      <Tabs.Screen name="profile" options={{ href: null }} />
      <Tabs.Screen name="explore" options={{ href: null }} />
    </Tabs>
  );
}

const styles = StyleSheet.create({
  scanFab: {
    top: -20,
    width: 60,
    height: 60,
    borderRadius: 30,
    backgroundColor: Brand.orange,
    alignItems: 'center',
    justifyContent: 'center',
    shadowColor: Brand.orangeDark,
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.5,
    shadowRadius: 8,
    elevation: 10,
  },
});

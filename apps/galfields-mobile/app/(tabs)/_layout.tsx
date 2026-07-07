import { Pressable, StyleSheet } from 'react-native';
import { Redirect, Tabs, router } from 'expo-router';
import { type BottomTabBarButtonProps } from '@react-navigation/bottom-tabs';
import * as Haptics from 'expo-haptics';
import { HapticTab } from '@/components/haptic-tab';
import { IconSymbol } from '@/components/ui/icon-symbol';
import { useAuth } from '@/contexts/auth-context';
import { Brand, Colors } from '@/constants/theme';
import { useColorScheme } from '@/hooks/use-color-scheme';

// The scan FAB bypasses the scan tab and goes directly to products/add,
// where the barcode scanner is integrated into the product form.
function ScanFab(_props: BottomTabBarButtonProps) {
  const handlePress = () => {
    if (process.env.EXPO_OS === 'ios') {
      Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Medium);
    }
    router.push('/products/add');
  };

  return (
    <Pressable onPress={handlePress} style={styles.scanFab} accessibilityRole="button">
      <IconSymbol name="barcode.viewfinder" size={28} color="#fff" />
    </Pressable>
  );
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
        tabBarLabelStyle: { fontSize: 11, fontWeight: '600' },
      }}
    >
      <Tabs.Screen
        name="index"
        options={{
          title: 'Inicio',
          tabBarIcon: ({ color }) => (
            <IconSymbol name="house.fill" size={26} color={color} />
          ),
        }}
      />
      <Tabs.Screen
        name="scan"
        options={{
          title: '',
          tabBarButton: ScanFab,
        }}
      />
      <Tabs.Screen
        name="profile"
        options={{
          title: 'Perfil',
          tabBarIcon: ({ color }) => (
            <IconSymbol name="person.fill" size={26} color={color} />
          ),
        }}
      />
      {/* Hide legacy explore screen from tab bar */}
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

import { useEffect, useRef } from 'react';
import {
  Animated,
  Modal,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  TouchableWithoutFeedback,
  View,
} from 'react-native';
import { router } from 'expo-router';
import { Brand } from '@/constants/theme';
import { useAuth } from '@/contexts/auth-context';
import { IconSymbol } from '@/components/ui/icon-symbol';

interface DrawerItem {
  label: string;
  icon: string;
  href?: string;
}

const DRAWER_ITEMS: DrawerItem[] = [
  { label: 'Inicio', icon: 'house.fill', href: '/(tabs)' },
  { label: 'Productos', icon: 'shippingbox.fill', href: '/products' },
  { label: 'Ventas', icon: 'chart.bar.fill' },
  { label: 'Inventario', icon: 'tray.fill' },
  { label: 'Carrito', icon: 'cart.fill' },
  { label: 'Historial', icon: 'clock.fill' },
  { label: 'Configuración', icon: 'gearshape.fill' },
];

const DRAWER_WIDTH = 280;

interface SideDrawerProps {
  visible: boolean;
  onClose: () => void;
  activeRoute?: string;
}

export function SideDrawer({ visible, onClose, activeRoute }: SideDrawerProps) {
  const { user, logout } = useAuth();
  const translateX = useRef(new Animated.Value(-DRAWER_WIDTH)).current;
  const opacity = useRef(new Animated.Value(0)).current;

  useEffect(() => {
    Animated.parallel([
      Animated.spring(translateX, {
        toValue: visible ? 0 : -DRAWER_WIDTH,
        useNativeDriver: true,
        damping: 22,
        stiffness: 140,
      }),
      Animated.timing(opacity, {
        toValue: visible ? 1 : 0,
        duration: 200,
        useNativeDriver: true,
      }),
    ]).start();
  }, [visible]);

  const handleItemPress = (item: DrawerItem) => {
    onClose();
    if (item.href) {
      // Small delay so the close animation starts before navigating
      setTimeout(() => router.push(item.href as any), 150);
    }
  };

  const handleLogout = () => {
    onClose();
    setTimeout(() => logout(), 300);
  };

  if (!visible) return null;

  return (
    <Modal visible transparent animationType="none" onRequestClose={onClose}>
      {/* Backdrop */}
      <TouchableWithoutFeedback onPress={onClose}>
        <Animated.View style={[styles.backdrop, { opacity }]} />
      </TouchableWithoutFeedback>

      {/* Drawer panel */}
      <Animated.View style={[styles.drawer, { transform: [{ translateX }] }]}>
        {/* Header */}
        <View style={styles.drawerHeader}>
          <View style={styles.avatar}>
            <Text style={styles.avatarInitial}>{(user?.username ?? 'C')[0]}</Text>
          </View>
          <Text style={styles.drawerName}>{user?.username ?? 'Cajero'}</Text>
          <Text style={styles.drawerRole}>{user?.role ?? 'Administrador'}</Text>
        </View>

        {/* Nav items */}
        <ScrollView style={styles.drawerBody} showsVerticalScrollIndicator={false}>
          {DRAWER_ITEMS.map(item => {
            const isActive = activeRoute === item.href;
            return (
              <Pressable
                key={item.label}
                onPress={() => handleItemPress(item)}
                style={[styles.drawerItem, isActive && styles.drawerItemActive]}
              >
                <IconSymbol
                  name={item.icon as any}
                  size={22}
                  color={isActive ? Brand.orange : '#444'}
                />
                <Text style={[styles.drawerItemLabel, isActive && styles.drawerItemLabelActive]}>
                  {item.label}
                </Text>
              </Pressable>
            );
          })}

          <View style={styles.separator} />

          <Pressable onPress={handleLogout} style={styles.drawerItem}>
            <IconSymbol name="arrow.right.square" size={22} color={Brand.danger} />
            <Text style={[styles.drawerItemLabel, styles.logoutLabel]}>Cerrar sesión</Text>
          </Pressable>
        </ScrollView>
      </Animated.View>
    </Modal>
  );
}

const styles = StyleSheet.create({
  backdrop: {
    ...StyleSheet.absoluteFillObject,
    backgroundColor: 'rgba(0,0,0,0.45)',
  },
  drawer: {
    position: 'absolute',
    top: 0,
    left: 0,
    width: DRAWER_WIDTH,
    height: '100%',
    backgroundColor: '#fff',
    shadowColor: '#000',
    shadowOffset: { width: 4, height: 0 },
    shadowOpacity: 0.15,
    shadowRadius: 12,
    elevation: 24,
  },
  drawerHeader: {
    backgroundColor: Brand.brown,
    paddingTop: 64,
    paddingBottom: 28,
    paddingHorizontal: 20,
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
  avatarInitial: {
    fontSize: 28,
    fontWeight: '700',
    color: '#fff',
  },
  drawerName: {
    fontSize: 20,
    fontWeight: '700',
    color: '#fff',
  },
  drawerRole: {
    fontSize: 13,
    color: 'rgba(255,255,255,0.65)',
    marginTop: 2,
  },
  drawerBody: { flex: 1, paddingTop: 8 },
  drawerItem: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 14,
    paddingHorizontal: 20,
    gap: 16,
  },
  drawerItemActive: {
    backgroundColor: `${Brand.orange}14`,
  },
  drawerItemLabel: {
    fontSize: 15,
    color: '#333',
    fontWeight: '500',
  },
  drawerItemLabelActive: {
    color: Brand.orange,
    fontWeight: '600',
  },
  separator: {
    height: StyleSheet.hairlineWidth,
    backgroundColor: '#EEE',
    marginHorizontal: 20,
    marginVertical: 8,
  },
  logoutLabel: {
    color: Brand.danger,
  },
});

// Fallback for using MaterialIcons on Android and web.

import MaterialIcons from '@expo/vector-icons/MaterialIcons';
import { SymbolWeight, SymbolViewProps } from 'expo-symbols';
import { ComponentProps } from 'react';
import { OpaqueColorValue, type StyleProp, type TextStyle } from 'react-native';

type IconMapping = Record<SymbolViewProps['name'], ComponentProps<typeof MaterialIcons>['name']>;
type IconSymbolName = keyof typeof MAPPING;

/**
 * SF Symbols → Material Icons mapping.
 * Add entries here when using a new icon in the app.
 */
const MAPPING = {
  // Navigation / structure
  'house.fill': 'home',
  'paperplane.fill': 'send',
  'chevron.left.forwardslash.chevron.right': 'code',
  'chevron.right': 'chevron-right',
  'chevron.down': 'expand-more',
  'arrow.left': 'arrow-back',
  'arrow.right.square': 'exit-to-app',
  'xmark': 'close',
  // Tab bar
  'barcode.viewfinder': 'qr-code-scanner',
  'person.fill': 'person',
  // Home / header
  'bell': 'notifications',
  'line.3.horizontal': 'menu',
  // Dashboard menu
  'square.grid.2x2.fill': 'apps',
  'chart.bar.fill': 'bar-chart',
  'tray.fill': 'archive',
  'cart.fill': 'shopping-cart',
  'clock.fill': 'history',
  'gearshape.fill': 'settings',
  // Products
  'shippingbox.fill': 'inventory',
  'magnifyingglass': 'search',
  'plus': 'add',
  // Forms
  'lock.fill': 'lock',
  'eye.fill': 'visibility',
  'eye.slash.fill': 'visibility-off',
  'camera.fill': 'camera-alt',
  // Success
  'checkmark.circle.fill': 'check-circle',
  // Catalog (categories / brands / locations)
  'pencil': 'edit',
  'trash.fill': 'delete',
  'tag.fill': 'sell',
  'building.2.fill': 'store',
  'mappin.and.ellipse': 'location-on',
} as IconMapping;

/**
 * An icon component that uses native SF Symbols on iOS, and Material Icons on Android and web.
 * Icon `name`s are based on SF Symbols; add new mappings to MAPPING above.
 */
export function IconSymbol({
  name,
  size = 24,
  color,
  style,
}: {
  name: IconSymbolName;
  size?: number;
  color: string | OpaqueColorValue;
  style?: StyleProp<TextStyle>;
  weight?: SymbolWeight;
}) {
  return <MaterialIcons color={color} size={size} name={MAPPING[name]} style={style} />;
}

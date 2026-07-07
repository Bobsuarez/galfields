import { Platform } from 'react-native';

export const Brand = {
  orange: '#E07820',
  orangeDark: '#B85E14',
  cream: '#FDF5E8',
  creamMid: '#F0E4CC',
  brown: '#3C1A0A',
  brownLight: '#5C2C14',
  success: '#52C41A',
  danger: '#E53935',
} as const;

export const Colors = {
  light: {
    text: '#1A1A1A',
    textSecondary: '#8A7060',
    background: Brand.cream,
    tint: Brand.orange,
    icon: '#8A7060',
    tabIconDefault: '#8A7060',
    tabIconSelected: Brand.orange,
    card: '#FFFFFF',
    border: '#E8DDD0',
    placeholder: '#B0A090',
  },
  dark: {
    text: '#F0E8DC',
    textSecondary: '#9A8870',
    background: '#180D05',
    tint: Brand.orange,
    icon: '#9A8870',
    tabIconDefault: '#9A8870',
    tabIconSelected: Brand.orange,
    card: '#2C1810',
    border: '#3C2A1C',
    placeholder: '#6A5840',
  },
} as const;

export const Fonts = Platform.select({
  ios: {
    sans: 'system-ui',
    serif: 'ui-serif',
    rounded: 'ui-rounded',
    mono: 'ui-monospace',
  },
  default: {
    sans: 'normal',
    serif: 'serif',
    rounded: 'normal',
    mono: 'monospace',
  },
  web: {
    sans: "system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif",
    serif: "Georgia, 'Times New Roman', serif",
    rounded: "'SF Pro Rounded', 'Hiragino Maru Gothic ProN', Meiryo, 'MS PGothic', sans-serif",
    mono: "SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', 'Courier New', monospace",
  },
});

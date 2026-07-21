import { Colors } from '@/constants/theme';
import { useColorScheme } from '@/hooks/use-color-scheme';

/** Full palette for the active scheme, for components that need several
 * theme colors at once (see `useThemeColor` for a single named lookup). */
export function useThemeColors() {
  const scheme = useColorScheme() ?? 'light';
  return Colors[scheme];
}

import { StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { IconSymbol } from '@/components/ui/icon-symbol';
import { Brand } from '@/constants/theme';

interface ComingSoonScreenProps {
  icon: string;
  title: string;
  message: string;
}

/** Placeholder for tabs whose real screen isn't built yet. */
export function ComingSoonScreen({ icon, title, message }: ComingSoonScreenProps) {
  const insets = useSafeAreaInsets();

  return (
    <View style={[styles.container, { paddingTop: insets.top + 40, paddingBottom: insets.bottom + 40 }]}>
      <View style={styles.iconBox}>
        <IconSymbol name={icon as any} size={40} color={Brand.orange} />
      </View>
      <Text style={styles.title}>{title}</Text>
      <Text style={styles.message}>{message}</Text>
      <View style={styles.badge}>
        <Text style={styles.badgeText}>Próximamente</Text>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: Brand.cream,
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: 32,
    gap: 12,
  },
  iconBox: {
    width: 88,
    height: 88,
    borderRadius: 44,
    backgroundColor: `${Brand.orange}15`,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 8,
  },
  title: {
    fontSize: 20,
    fontWeight: '700',
    color: '#1A1A1A',
    textAlign: 'center',
  },
  message: {
    fontSize: 14,
    color: '#8A7060',
    textAlign: 'center',
    lineHeight: 20,
  },
  badge: {
    marginTop: 12,
    paddingHorizontal: 14,
    paddingVertical: 6,
    borderRadius: 20,
    backgroundColor: `${Brand.orange}1A`,
  },
  badgeText: {
    fontSize: 12,
    fontWeight: '700',
    color: Brand.orange,
    textTransform: 'uppercase',
    letterSpacing: 0.6,
  },
});

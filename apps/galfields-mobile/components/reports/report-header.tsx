import { Pressable, StyleSheet, Text, View } from 'react-native';
import { router } from 'expo-router';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { IconSymbol } from '@/components/ui/icon-symbol';
import { Brand } from '@/constants/theme';

interface ReportHeaderProps {
  title: string;
}

/** Same back-arrow + centered-title header every settings/report screen
 * uses (see app/settings/index.tsx) — extracted here since every report
 * screen repeats it identically. */
export function ReportHeader({ title }: ReportHeaderProps) {
  const insets = useSafeAreaInsets();

  return (
    <View style={[styles.header, { paddingTop: insets.top + 8 }]}>
      <Pressable onPress={() => router.back()} hitSlop={10}>
        <IconSymbol name="arrow.left" size={24} color="#fff" />
      </Pressable>
      <Text style={styles.title}>{title}</Text>
      <View style={styles.spacer} />
    </View>
  );
}

const styles = StyleSheet.create({
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 16,
    paddingBottom: 16,
    backgroundColor: Brand.brown,
    gap: 12,
  },
  title: { flex: 1, fontSize: 20, fontWeight: '700', color: '#fff', textAlign: 'center' },
  spacer: { width: 24 },
});

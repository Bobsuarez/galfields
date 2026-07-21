import { StyleSheet, Text, View } from 'react-native';
import { Brand } from '@/constants/theme';
import { useThemeColors } from '@/hooks/use-theme-colors';

interface StatCardProps {
  label: string;
  value: string;
  accent?: boolean;
}

export function StatCard({ label, value, accent }: StatCardProps) {
  const colors = useThemeColors();
  return (
    <View style={[styles.card, { backgroundColor: colors.card }, accent && styles.cardAccent]}>
      <Text
        style={[styles.value, { color: colors.text }, accent && styles.valueAccent]}
        numberOfLines={1}
        adjustsFontSizeToFit
      >
        {value}
      </Text>
      <Text style={[styles.label, { color: colors.textSecondary }, accent && styles.labelAccent]}>{label}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  card: {
    flex: 1,
    borderRadius: 14,
    paddingVertical: 16,
    paddingHorizontal: 10,
    alignItems: 'center',
    gap: 4,
  },
  cardAccent: { backgroundColor: Brand.orange },
  value: { fontSize: 19, fontWeight: '700' },
  valueAccent: { color: '#fff' },
  label: { fontSize: 12, textAlign: 'center' },
  labelAccent: { color: 'rgba(255,255,255,0.85)' },
});

import { StyleSheet, Text, View } from 'react-native';
import { Brand } from '@/constants/theme';

interface StatCardProps {
  label: string;
  value: string;
  accent?: boolean;
}

export function StatCard({ label, value, accent }: StatCardProps) {
  return (
    <View style={[styles.card, accent && styles.cardAccent]}>
      <Text style={[styles.value, accent && styles.valueAccent]} numberOfLines={1} adjustsFontSizeToFit>
        {value}
      </Text>
      <Text style={[styles.label, accent && styles.labelAccent]}>{label}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  card: {
    flex: 1,
    backgroundColor: '#fff',
    borderRadius: 14,
    paddingVertical: 16,
    paddingHorizontal: 10,
    alignItems: 'center',
    gap: 4,
  },
  cardAccent: { backgroundColor: Brand.orange },
  value: { fontSize: 19, fontWeight: '700', color: '#1A1A1A' },
  valueAccent: { color: '#fff' },
  label: { fontSize: 12, color: '#8A7060', textAlign: 'center' },
  labelAccent: { color: 'rgba(255,255,255,0.85)' },
});

import { StyleSheet, Text, View } from 'react-native';
import { Brand } from '@/constants/theme';
import { formatCurrency } from '@/utils/currency';

interface BarRowProps {
  label: string;
  amount: number;
  maxAmount: number;
  subtitle?: string;
}

/** Simple `View`-based horizontal bar — no chart library is installed in
 * this app, and a proportional bar per row covers "compare these amounts"
 * without adding one. */
export function BarRow({ label, amount, maxAmount, subtitle }: BarRowProps) {
  const pct = maxAmount > 0 ? Math.max(4, (amount / maxAmount) * 100) : 0;

  return (
    <View style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.label}>{label}</Text>
        <Text style={styles.amount}>{formatCurrency(amount)}</Text>
      </View>
      <View style={styles.track}>
        <View style={[styles.fill, { width: `${pct}%` }]} />
      </View>
      {subtitle ? <Text style={styles.subtitle}>{subtitle}</Text> : null}
    </View>
  );
}

const styles = StyleSheet.create({
  container: { marginBottom: 14 },
  header: { flexDirection: 'row', justifyContent: 'space-between', marginBottom: 4 },
  label: { fontSize: 14, fontWeight: '600', color: '#1A1A1A' },
  amount: { fontSize: 14, fontWeight: '700', color: Brand.orange },
  track: { height: 8, borderRadius: 4, backgroundColor: '#F0E4CC', overflow: 'hidden' },
  fill: { height: '100%', borderRadius: 4, backgroundColor: Brand.orange },
  subtitle: { fontSize: 11, color: '#8A7060', marginTop: 2 },
});

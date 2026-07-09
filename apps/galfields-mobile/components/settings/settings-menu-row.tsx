import { Pressable, StyleSheet, Text, View } from 'react-native';
import { Brand } from '@/constants/theme';
import { IconSymbol } from '@/components/ui/icon-symbol';

interface SettingsMenuRowProps {
  icon: string;
  label: string;
  subtitle: string;
  onPress?: () => void;
}

export function SettingsMenuRow({ icon, label, subtitle, onPress }: SettingsMenuRowProps) {
  return (
    <Pressable
      onPress={onPress}
      style={({ pressed }) => [styles.row, pressed && styles.pressed]}
    >
      <View style={styles.iconBox}>
        <IconSymbol name={icon as any} size={24} color={Brand.orange} />
      </View>
      <View style={styles.info}>
        <Text style={styles.label}>{label}</Text>
        <Text style={styles.subtitle}>{subtitle}</Text>
      </View>
      <IconSymbol name="chevron.right" size={20} color="#CCC" />
    </Pressable>
  );
}

const styles = StyleSheet.create({
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#fff',
    paddingVertical: 14,
    paddingHorizontal: 16,
    gap: 14,
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: '#EEE8E0',
  },
  pressed: { opacity: 0.7 },
  iconBox: {
    width: 44,
    height: 44,
    borderRadius: 12,
    backgroundColor: `${Brand.orange}1A`,
    alignItems: 'center',
    justifyContent: 'center',
  },
  info: { flex: 1 },
  label: { fontSize: 15, fontWeight: '600', color: '#1A1A1A' },
  subtitle: { fontSize: 12, color: '#8A7060', marginTop: 2 },
});

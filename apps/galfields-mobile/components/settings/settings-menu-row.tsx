import { Pressable, StyleSheet, Text, View } from 'react-native';
import { Brand } from '@/constants/theme';
import { useThemeColors } from '@/hooks/use-theme-colors';
import { IconSymbol } from '@/components/ui/icon-symbol';

interface SettingsMenuRowProps {
  icon: string;
  label: string;
  subtitle: string;
  onPress?: () => void;
}

export function SettingsMenuRow({ icon, label, subtitle, onPress }: SettingsMenuRowProps) {
  const colors = useThemeColors();
  return (
    <Pressable
      onPress={onPress}
      style={({ pressed }) => [
        styles.row,
        { backgroundColor: colors.card, borderBottomColor: colors.border },
        pressed && styles.pressed,
      ]}
    >
      <View style={styles.iconBox}>
        <IconSymbol name={icon as any} size={24} color={Brand.orange} />
      </View>
      <View style={styles.info}>
        <Text style={[styles.label, { color: colors.text }]}>{label}</Text>
        <Text style={[styles.subtitle, { color: colors.textSecondary }]}>{subtitle}</Text>
      </View>
      <IconSymbol name="chevron.right" size={20} color={colors.icon} />
    </Pressable>
  );
}

const styles = StyleSheet.create({
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 14,
    paddingHorizontal: 16,
    gap: 14,
    borderBottomWidth: StyleSheet.hairlineWidth,
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
  label: { fontSize: 15, fontWeight: '600' },
  subtitle: { fontSize: 12, marginTop: 2 },
});

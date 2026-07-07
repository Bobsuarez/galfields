import { ActivityIndicator, Pressable, StyleSheet, Text } from 'react-native';
import { Brand } from '@/constants/theme';

interface AppButtonProps {
  label: string;
  onPress: () => void;
  variant?: 'primary' | 'outline';
  loading?: boolean;
  disabled?: boolean;
}

export function AppButton({
  label,
  onPress,
  variant = 'primary',
  loading,
  disabled,
}: AppButtonProps) {
  const isPrimary = variant === 'primary';

  return (
    <Pressable
      onPress={onPress}
      disabled={disabled || loading}
      style={({ pressed }) => [
        styles.base,
        isPrimary ? styles.primary : styles.outline,
        (pressed || disabled) && styles.dimmed,
      ]}
    >
      {loading ? (
        <ActivityIndicator color={isPrimary ? '#fff' : Brand.orange} />
      ) : (
        <Text style={[styles.label, !isPrimary && styles.outlineLabel]}>{label}</Text>
      )}
    </Pressable>
  );
}

const styles = StyleSheet.create({
  base: {
    height: 52,
    borderRadius: 12,
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: 24,
  },
  primary: {
    backgroundColor: Brand.orange,
  },
  outline: {
    borderWidth: 1.5,
    borderColor: Brand.orange,
    backgroundColor: 'transparent',
  },
  dimmed: { opacity: 0.7 },
  label: {
    fontSize: 16,
    fontWeight: '600',
    color: '#fff',
    letterSpacing: 0.2,
  },
  outlineLabel: {
    color: Brand.orange,
  },
});

import { Pressable, StyleSheet, Text, TextInput, type TextInputProps, View } from 'react-native';
import { Brand, Colors } from '@/constants/theme';
import { useColorScheme } from '@/hooks/use-color-scheme';
import { IconSymbol } from './icon-symbol';

interface TextInputFieldProps extends TextInputProps {
  label?: string;
  leftIcon?: string;
  rightIcon?: string;
  onRightIconPress?: () => void;
  error?: string;
}

export function TextInputField({
  label,
  leftIcon,
  rightIcon,
  onRightIconPress,
  error,
  style,
  ...props
}: TextInputFieldProps) {
  const scheme = useColorScheme() ?? 'light';
  const colors = Colors[scheme];

  return (
    <View style={styles.wrapper}>
      {label && <Text style={[styles.label, { color: colors.text }]}>{label}</Text>}
      <View
        style={[
          styles.row,
          {
            borderColor: error ? Brand.danger : colors.border,
            backgroundColor: colors.card,
          },
        ]}
      >
        {leftIcon && (
          <IconSymbol
            name={leftIcon as any}
            size={20}
            color={colors.icon}
            style={styles.leftIcon}
          />
        )}
        <TextInput
          {...props}
          style={[styles.input, { color: colors.text }, style]}
          placeholderTextColor={colors.placeholder}
        />
        {rightIcon && (
          <Pressable onPress={onRightIconPress} hitSlop={8}>
            <IconSymbol name={rightIcon as any} size={20} color={colors.icon} />
          </Pressable>
        )}
      </View>
      {error ? <Text style={styles.error}>{error}</Text> : null}
    </View>
  );
}

const styles = StyleSheet.create({
  wrapper: { marginBottom: 14 },
  label: { fontSize: 13, fontWeight: '500', marginBottom: 6 },
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    borderWidth: 1,
    borderRadius: 10,
    paddingHorizontal: 14,
    height: 50,
  },
  leftIcon: { marginRight: 10 },
  input: { flex: 1, fontSize: 15, paddingVertical: 0 },
  error: { fontSize: 12, color: Brand.danger, marginTop: 4 },
});

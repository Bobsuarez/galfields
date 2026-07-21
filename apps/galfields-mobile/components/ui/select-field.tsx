import { FlatList, Modal, Pressable, StyleSheet, Text, View } from 'react-native';
import { Brand, Colors } from '@/constants/theme';
import { useColorScheme } from '@/hooks/use-color-scheme';
import { useState } from 'react';
import { IconSymbol } from './icon-symbol';

interface SelectFieldProps {
  label?: string;
  value: string;
  options: string[];
  placeholder?: string;
  onSelect: (value: string) => void;
  error?: string;
}

export function SelectField({
  label,
  value,
  options,
  placeholder = 'Selecciona una opción',
  onSelect,
  error,
}: SelectFieldProps) {
  const [open, setOpen] = useState(false);
  const scheme = useColorScheme() ?? 'light';
  const colors = Colors[scheme];

  const handleSelect = (option: string) => {
    onSelect(option);
    setOpen(false);
  };

  return (
    <View style={styles.wrapper}>
      {label && <Text style={[styles.label, { color: colors.text }]}>{label}</Text>}
      <Pressable
        onPress={() => setOpen(true)}
        style={[
          styles.trigger,
          { borderColor: error ? Brand.danger : colors.border, backgroundColor: colors.card },
        ]}
      >
        <Text style={[styles.triggerText, { color: value ? colors.text : colors.placeholder }]}>
          {value || placeholder}
        </Text>
        <IconSymbol name="chevron.down" size={18} color={colors.icon} />
      </Pressable>
      {error ? <Text style={styles.error}>{error}</Text> : null}

      <Modal visible={open} transparent animationType="fade" onRequestClose={() => setOpen(false)}>
        <Pressable style={styles.backdrop} onPress={() => setOpen(false)} />
        <View style={[styles.sheet, { backgroundColor: colors.card }]}>
          <Text style={[styles.sheetTitle, { color: colors.text }]}>
            {label ?? 'Seleccionar'}
          </Text>
          <FlatList
            data={options}
            keyExtractor={(item, index) => `${item}-${index}`}
            renderItem={({ item }) => (
              <Pressable
                onPress={() => handleSelect(item)}
                style={[styles.option, item === value && styles.optionSelected]}
              >
                <Text
                  style={[
                    styles.optionText,
                    { color: colors.text },
                    item === value && styles.optionTextSelected,
                  ]}
                >
                  {item}
                </Text>
                {item === value && (
                  <IconSymbol name="checkmark.circle.fill" size={20} color={Brand.orange} />
                )}
              </Pressable>
            )}
          />
        </View>
      </Modal>
    </View>
  );
}

const styles = StyleSheet.create({
  wrapper: { marginBottom: 14 },
  label: { fontSize: 13, fontWeight: '500', marginBottom: 6 },
  trigger: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    borderWidth: 1,
    borderRadius: 10,
    paddingHorizontal: 14,
    height: 50,
  },
  triggerText: { fontSize: 15, flex: 1 },
  error: { fontSize: 12, color: Brand.danger, marginTop: 4 },
  backdrop: {
    ...StyleSheet.absoluteFillObject,
    backgroundColor: 'rgba(0,0,0,0.4)',
  },
  sheet: {
    position: 'absolute',
    bottom: 0,
    left: 0,
    right: 0,
    borderTopLeftRadius: 20,
    borderTopRightRadius: 20,
    paddingTop: 20,
    paddingBottom: 40,
    maxHeight: '60%',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: -4 },
    shadowOpacity: 0.12,
    shadowRadius: 12,
    elevation: 20,
  },
  sheetTitle: {
    fontSize: 16,
    fontWeight: '700',
    paddingHorizontal: 20,
    marginBottom: 12,
  },
  option: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 14,
    paddingHorizontal: 20,
    justifyContent: 'space-between',
  },
  optionSelected: { backgroundColor: `${Brand.orange}12` },
  optionText: { fontSize: 15 },
  optionTextSelected: { color: Brand.orange, fontWeight: '600' },
});

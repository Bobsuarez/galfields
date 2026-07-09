import { useEffect, useState } from 'react';
import {
  KeyboardAvoidingView,
  Modal,
  Platform,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import { AppButton } from '@/components/ui/app-button';
import { TextInputField } from '@/components/ui/text-input-field';
import { Colors } from '@/constants/theme';
import { useColorScheme } from '@/hooks/use-color-scheme';

export interface CatalogFormField {
  key: string;
  label: string;
  placeholder?: string;
  required?: boolean;
  keyboardType?: 'default' | 'numeric' | 'phone-pad';
  multiline?: boolean;
}

interface CatalogFormModalProps {
  visible: boolean;
  title: string;
  fields: CatalogFormField[];
  initialValues?: Record<string, string>;
  saving?: boolean;
  onSave: (values: Record<string, string>) => void;
  onCancel: () => void;
}

/**
 * Generic add/edit form for the catalog screens (categories, brands,
 * locations) — the fields differ per entity, so callers pass a field list
 * instead of this component knowing about any specific entity shape.
 */
export function CatalogFormModal({
  visible,
  title,
  fields,
  initialValues,
  saving,
  onSave,
  onCancel,
}: CatalogFormModalProps) {
  const scheme = useColorScheme() ?? 'light';
  const colors = Colors[scheme];
  const [values, setValues] = useState<Record<string, string>>({});
  const [errors, setErrors] = useState<Record<string, string>>({});

  useEffect(() => {
    if (visible) {
      setValues(Object.fromEntries(fields.map(f => [f.key, initialValues?.[f.key] ?? ''])));
      setErrors({});
    }
  }, [visible, initialValues, fields]);

  const setValue = (key: string, value: string) => {
    setValues(prev => ({ ...prev, [key]: value }));
    setErrors(prev => ({ ...prev, [key]: '' }));
  };

  const handleSave = () => {
    const nextErrors: Record<string, string> = {};
    fields.forEach(field => {
      if (field.required && !values[field.key]?.trim()) {
        nextErrors[field.key] = `${field.label} es requerido`;
      }
    });
    setErrors(nextErrors);
    if (Object.keys(nextErrors).length > 0) return;
    onSave(values);
  };

  return (
    <Modal visible={visible} transparent animationType="fade" onRequestClose={onCancel}>
      <Pressable style={styles.backdrop} onPress={onCancel} />
      <KeyboardAvoidingView
        behavior={Platform.OS === 'ios' ? 'padding' : undefined}
        style={styles.sheetWrap}
      >
        <View style={[styles.sheet, { backgroundColor: colors.card }]}>
          <Text style={[styles.sheetTitle, { color: colors.text }]}>{title}</Text>
          <ScrollView keyboardShouldPersistTaps="handled">
            {fields.map(field => (
              <TextInputField
                key={field.key}
                label={field.label}
                placeholder={field.placeholder}
                value={values[field.key] ?? ''}
                onChangeText={t => setValue(field.key, t)}
                error={errors[field.key]}
                keyboardType={field.keyboardType}
                multiline={field.multiline}
              />
            ))}
          </ScrollView>
          <View style={styles.footer}>
            <View style={styles.footerBtn}>
              <AppButton label="Cancelar" variant="outline" onPress={onCancel} disabled={saving} />
            </View>
            <View style={styles.footerBtn}>
              <AppButton label="Guardar" onPress={handleSave} loading={saving} />
            </View>
          </View>
        </View>
      </KeyboardAvoidingView>
    </Modal>
  );
}

const styles = StyleSheet.create({
  backdrop: {
    ...StyleSheet.absoluteFillObject,
    backgroundColor: 'rgba(0,0,0,0.4)',
  },
  sheetWrap: {
    position: 'absolute',
    bottom: 0,
    left: 0,
    right: 0,
  },
  sheet: {
    borderTopLeftRadius: 20,
    borderTopRightRadius: 20,
    paddingTop: 20,
    paddingHorizontal: 20,
    paddingBottom: 32,
    maxHeight: '80%',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: -4 },
    shadowOpacity: 0.12,
    shadowRadius: 12,
    elevation: 20,
  },
  sheetTitle: { fontSize: 16, fontWeight: '700', marginBottom: 16 },
  footer: { flexDirection: 'row', gap: 12, marginTop: 8 },
  footerBtn: { flex: 1 },
});

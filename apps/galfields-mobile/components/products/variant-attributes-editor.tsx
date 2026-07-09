import { useState } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { TextInputField } from '@/components/ui/text-input-field';
import { SelectField } from '@/components/ui/select-field';
import { IconSymbol } from '@/components/ui/icon-symbol';
import { Brand } from '@/constants/theme';
import type { ProductVariantAttribute } from '@/types/product';

const ATTRIBUTE_NAME_SUGGESTIONS = ['Color', 'Talla', 'Sabor', 'Tamaño', 'Peso', 'Material', 'Presentación'];
const MEASUREMENT_UNITS = ['gr', 'kg', 'lt', 'ml', 'oz'];
const MEASUREMENT_ATTRIBUTE_NAMES = ['tamaño', 'peso'];

function isMeasurementAttribute(name: string): boolean {
  return MEASUREMENT_ATTRIBUTE_NAMES.includes(name.trim().toLowerCase());
}

interface AttributeRowProps {
  attribute: ProductVariantAttribute;
  onChange: (next: ProductVariantAttribute) => void;
  onRemove: () => void;
}

/** Plain attributes (Color, Sabor, ...) take a free-text value. Tamaño/Peso
 * switch to a number + unit combo so the stored value stays a single
 * uniform string (e.g. "48gr") instead of freeform text in the DB. */
function AttributeRow({ attribute, onChange, onRemove }: AttributeRowProps) {
  const measurement = isMeasurementAttribute(attribute.name);
  const [amount, setAmount] = useState('');
  const [unit, setUnit] = useState('gr');

  const updateName = (name: string) => {
    // Changing the attribute (e.g. Tamaño -> Color) invalidates whatever
    // value was already entered, whichever mode it came from - always start
    // the value fresh instead of carrying over "45gr" into "Color".
    setAmount('');
    onChange({ ...attribute, name, value: '' });
  };

  const updateAmount = (text: string) => {
    const cleaned = text.replace(/[^0-9.]/g, '');
    setAmount(cleaned);
    onChange({ ...attribute, value: cleaned ? `${cleaned}${unit}` : '' });
  };

  const updateUnit = (nextUnit: string) => {
    setUnit(nextUnit);
    onChange({ ...attribute, value: amount ? `${amount}${nextUnit}` : '' });
  };

  return (
    <View style={styles.attributeBlock}>
      <View style={styles.attributeHeader}>
        <TextInputField
          placeholder="Nombre del atributo"
          value={attribute.name}
          onChangeText={updateName}
        />
        <Pressable onPress={onRemove} hitSlop={8} style={styles.removeBtn}>
          <IconSymbol name="xmark" size={16} color={Brand.danger} />
        </Pressable>
      </View>

      <View style={styles.chipsRow}>
        {ATTRIBUTE_NAME_SUGGESTIONS.map(suggestion => (
          <Pressable
            key={suggestion}
            onPress={() => updateName(suggestion)}
            style={[styles.chip, attribute.name === suggestion && styles.chipActive]}
          >
            <Text style={[styles.chipText, attribute.name === suggestion && styles.chipTextActive]}>
              {suggestion}
            </Text>
          </Pressable>
        ))}
      </View>

      {measurement ? (
        <View style={styles.measurementRow}>
          <View style={styles.measurementAmount}>
            <TextInputField
              placeholder="Cantidad"
              value={amount}
              onChangeText={updateAmount}
              keyboardType="numeric"
            />
          </View>
          <View style={styles.measurementUnit}>
            <SelectField value={unit} options={MEASUREMENT_UNITS} onSelect={updateUnit} />
          </View>
        </View>
      ) : (
        <TextInputField
          placeholder="Valor (ej. Rojo)"
          value={attribute.value}
          onChangeText={value => onChange({ ...attribute, value })}
        />
      )}
    </View>
  );
}

interface VariantAttributesEditorProps {
  attributes: ProductVariantAttribute[];
  error?: string;
  onChange: (next: ProductVariantAttribute[]) => void;
}

export function VariantAttributesEditor({ attributes, error, onChange }: VariantAttributesEditorProps) {
  const updateAttribute = (index: number, next: ProductVariantAttribute) => {
    onChange(attributes.map((attr, i) => (i === index ? next : attr)));
  };

  const removeAttribute = (index: number) => {
    onChange(attributes.filter((_, i) => i !== index));
  };

  const addAttribute = () => {
    onChange([...attributes, { name: '', value: '' }]);
  };

  return (
    <View style={styles.wrapper}>
      <Text style={styles.label}>Atributos (requerido, ej. tamaño o color)</Text>
      {attributes.map((attr, index) => (
        <AttributeRow
          key={index}
          attribute={attr}
          onChange={next => updateAttribute(index, next)}
          onRemove={() => removeAttribute(index)}
        />
      ))}
      {error ? <Text style={styles.errorText}>{error}</Text> : null}
      <Pressable onPress={addAttribute} style={styles.addBtn}>
        <IconSymbol name="plus" size={16} color={Brand.orange} />
        <Text style={styles.addBtnText}>Agregar atributo</Text>
      </Pressable>
    </View>
  );
}

const styles = StyleSheet.create({
  wrapper: { marginBottom: 4 },
  label: { fontSize: 13, fontWeight: '500', marginBottom: 6, color: '#1A1A1A' },
  errorText: { fontSize: 12, color: Brand.danger, marginBottom: 8 },

  attributeBlock: {
    borderWidth: 1,
    borderColor: '#E8DDD0',
    borderRadius: 10,
    padding: 10,
    marginBottom: 10,
  },
  attributeHeader: { flexDirection: 'row', alignItems: 'flex-start', gap: 8 },
  removeBtn: { padding: 8, marginTop: 2 },

  chipsRow: { flexDirection: 'row', flexWrap: 'wrap', gap: 6, marginTop: -6, marginBottom: 10 },
  chip: {
    paddingVertical: 5,
    paddingHorizontal: 10,
    borderRadius: 14,
    backgroundColor: `${Brand.orange}12`,
  },
  chipActive: { backgroundColor: Brand.orange },
  chipText: { fontSize: 12, color: Brand.orange, fontWeight: '500' },
  chipTextActive: { color: '#fff' },

  measurementRow: { flexDirection: 'row', gap: 10 },
  measurementAmount: { flex: 1 },
  measurementUnit: { width: 110 },

  addBtn: { flexDirection: 'row', alignItems: 'center', gap: 6, paddingVertical: 6 },
  addBtnText: { fontSize: 13, color: Brand.orange, fontWeight: '600' },
});

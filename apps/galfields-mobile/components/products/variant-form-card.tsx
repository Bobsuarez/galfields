import { Pressable, StyleSheet, Text, View } from 'react-native';
import { TextInputField } from '@/components/ui/text-input-field';
import { IconSymbol } from '@/components/ui/icon-symbol';
import { ImagePickerField } from '@/components/products/image-picker-field';
import { VariantAttributesEditor } from '@/components/products/variant-attributes-editor';
import { useImagePicker } from '@/hooks/use-image-picker';
import { buildVariantSku } from '@/utils/sku';
import { Brand } from '@/constants/theme';
import type { ProductVariantDraft } from '@/types/product';

export interface VariantFormErrors {
  barcode?: string;
  price?: string;
  costPrice?: string;
  initialStock?: string;
  attributes?: string;
}

interface VariantFormCardProps {
  index: number;
  productName: string;
  value: ProductVariantDraft;
  errors?: VariantFormErrors;
  canRemove: boolean;
  onChange: (next: ProductVariantDraft) => void;
  onRemove: () => void;
  onScanBarcode: () => void;
}

export function VariantFormCard({
  index,
  productName,
  value,
  errors,
  canRemove,
  onChange,
  onRemove,
  onScanBarcode,
}: VariantFormCardProps) {
  const { processing, pick, clear } = useImagePicker(imageUri => onChange({ ...value, imageUri: imageUri ?? undefined }));
  const sku = value.originalSku ?? buildVariantSku(productName, value.attributes[0]?.value ?? '', value.barcode);

  return (
    <View style={styles.card}>
      <View style={styles.header}>
        <Text style={styles.title}>Variante {index + 1}</Text>
        {canRemove && (
          <Pressable onPress={onRemove} hitSlop={8}>
            <IconSymbol name="trash.fill" size={18} color={Brand.danger} />
          </Pressable>
        )}
      </View>

      <View style={styles.body}>
        <ImagePickerField
          imageUri={value.imageUri}
          processing={processing}
          onPick={pick}
          onRemove={clear}
          compact
        />

        <View style={styles.fields}>
          <TextInputField
            label="SKU (generado automáticamente)"
            placeholder="Se completa con nombre, atributo y código de barras"
            value={sku}
            editable={false}
            style={styles.skuInput}
          />

          <View style={styles.barcodeRow}>
            <View style={styles.barcodeInput}>
              <TextInputField
                label="Código de barras"
                placeholder="Ingresa o escanea"
                value={value.barcode}
                onChangeText={t => onChange({ ...value, barcode: t })}
                keyboardType="number-pad"
                error={errors?.barcode}
              />
            </View>
            <Pressable
              onPress={onScanBarcode}
              style={({ pressed }) => [styles.scanBtn, pressed && styles.scanBtnPressed]}
              accessibilityLabel="Escanear código de barras"
            >
              <IconSymbol name="barcode.viewfinder" size={22} color="#fff" />
            </Pressable>
          </View>
        </View>
      </View>

      <View style={styles.priceRow}>
        <View style={styles.priceField}>
          <TextInputField
            label="Precio de venta"
            placeholder="0"
            value={value.price}
            onChangeText={t => onChange({ ...value, price: t.replace(/[^0-9]/g, '') })}
            keyboardType="numeric"
            error={errors?.price}
          />
        </View>
        <View style={styles.priceField}>
          <TextInputField
            label="Costo"
            placeholder="0"
            value={value.costPrice}
            onChangeText={t => onChange({ ...value, costPrice: t.replace(/[^0-9]/g, '') })}
            keyboardType="numeric"
            error={errors?.costPrice}
          />
        </View>
      </View>

      <TextInputField
        label="Stock inicial"
        placeholder="0"
        value={value.initialStock}
        onChangeText={t => onChange({ ...value, initialStock: t.replace(/[^0-9]/g, '') })}
        keyboardType="numeric"
        error={errors?.initialStock}
      />

      <VariantAttributesEditor
        attributes={value.attributes}
        error={errors?.attributes}
        onChange={attributes => onChange({ ...value, attributes })}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  card: {
    backgroundColor: '#fff',
    borderRadius: 14,
    padding: 16,
    marginBottom: 14,
    borderWidth: 1,
    borderColor: '#E8DDD0',
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginBottom: 12,
  },
  title: { fontSize: 14, fontWeight: '700', color: Brand.orange },
  body: { flexDirection: 'row', gap: 12, marginBottom: 4 },
  fields: { flex: 1 },
  barcodeRow: { flexDirection: 'row', alignItems: 'flex-start', gap: 10 },
  barcodeInput: { flex: 1 },
  scanBtn: {
    width: 50,
    height: 50,
    borderRadius: 10,
    backgroundColor: Brand.orange,
    alignItems: 'center',
    justifyContent: 'center',
    marginTop: 22,
  },
  scanBtnPressed: { opacity: 0.75 },
  priceRow: { flexDirection: 'row', gap: 12 },
  priceField: { flex: 1 },
  skuInput: { color: '#8A7060' },
});

import { useState } from 'react';
import {
  ActivityIndicator,
  Alert,
  Image,
  KeyboardAvoidingView,
  Platform,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import { router } from 'expo-router';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import * as ImagePicker from 'expo-image-picker';
import { AppButton } from '@/components/ui/app-button';
import { TextInputField } from '@/components/ui/text-input-field';
import { SelectField } from '@/components/ui/select-field';
import { IconSymbol } from '@/components/ui/icon-symbol';
import { BarcodeScannerModal } from '@/components/products/barcode-scanner-modal';
import { removeBackground } from '@/services/background-removal';
import { useProducts } from '@/contexts/products-context';
import { Brand } from '@/constants/theme';
import {
  PRODUCT_CATEGORIES,
  PRODUCT_UNITS,
  type ProductCategory,
  type ProductFormData,
  type ProductUnit,
} from '@/types/product';

interface FormErrors {
  name?: string;
  category?: string;
  price?: string;
  stock?: string;
  unit?: string;
  barcode?: string;
}

export default function AddProductScreen() {
  const { addProduct } = useProducts();
  const insets = useSafeAreaInsets();

  const [name, setName] = useState('');
  const [category, setCategory] = useState('');
  const [price, setPrice] = useState('');
  const [stock, setStock] = useState('');
  const [unit, setUnit] = useState('');
  const [barcode, setBarcode] = useState('');
  const [imageUri, setImageUri] = useState<string | null>(null);
  const [imageProcessing, setImageProcessing] = useState(false);
  const [scannerOpen, setScannerOpen] = useState(false);
  const [errors, setErrors] = useState<FormErrors>({});
  const [saving, setSaving] = useState(false);

  const clearError = (field: keyof FormErrors) =>
    setErrors(prev => ({ ...prev, [field]: undefined }));

  const validate = (): boolean => {
    const next: FormErrors = {};
    if (!name.trim()) next.name = 'El nombre es requerido';
    if (!category) next.category = 'Selecciona una categoría';
    if (!price.trim() || isNaN(Number(price)) || Number(price) <= 0)
      next.price = 'Ingresa un precio válido';
    if (!stock.trim() || isNaN(Number(stock)) || Number(stock) < 0)
      next.stock = 'Ingresa un stock válido';
    if (!unit) next.unit = 'Selecciona una unidad';
    if (!barcode.trim()) next.barcode = 'El código de barras es requerido';
    setErrors(next);
    return Object.keys(next).length === 0;
  };

  const handlePickImage = async (source: 'camera' | 'gallery') => {
    const result =
      source === 'camera'
        ? await ImagePicker.launchCameraAsync({
            mediaTypes: ['images'],
            quality: 0.85,
            allowsEditing: false,
          })
        : await ImagePicker.launchImageLibraryAsync({
            mediaTypes: ['images'],
            quality: 0.85,
            allowsEditing: false,
          });

    if (result.canceled) return;

    const uri = result.assets[0].uri;
    setImageProcessing(true);
    setImageUri(null);
    try {
      const processedUri = await removeBackground(uri);
      setImageUri(processedUri);
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'Error desconocido';
      Alert.alert(
        'No se pudo remover el fondo',
        `${msg}\n\nSe usará la imagen original.`,
        [{ text: 'OK' }],
      );
      // Fallback: keep the original photo
      setImageUri(uri);
    } finally {
      setImageProcessing(false);
    }
  };

  const showImageOptions = () => {
    Alert.alert('Imagen del producto', 'Elige una opción', [
      { text: 'Tomar foto', onPress: () => handlePickImage('camera') },
      { text: 'Seleccionar de galería', onPress: () => handlePickImage('gallery') },
      ...(imageUri ? [{ text: 'Eliminar imagen', style: 'destructive' as const, onPress: () => setImageUri(null) }] : []),
      { text: 'Cancelar', style: 'cancel' },
    ]);
  };

  const handleSave = async () => {
    if (!validate()) return;
    setSaving(true);
    try {
      const data: ProductFormData = {
        name: name.trim(),
        category: category as ProductCategory,
        price: Number(price),
        stock: Number(stock),
        unit: unit as ProductUnit,
        barcode: barcode.trim(),
        imageUri: imageUri ?? undefined,
      };
      const product = await addProduct(data);
      router.replace({
        pathname: '/products/success',
        params: {
          name: product.name,
          price: String(product.price),
          stock: String(product.stock),
        },
      });
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'Error desconocido';
      Alert.alert('No se pudo guardar el producto', msg, [{ text: 'OK' }]);
    } finally {
      setSaving(false);
    }
  };

  return (
    <KeyboardAvoidingView
      behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
      style={styles.flex}
    >
      {/* Header */}
      <View style={[styles.header, { paddingTop: insets.top + 8 }]}>
        <Pressable onPress={() => router.back()} hitSlop={10}>
          <IconSymbol name="arrow.left" size={24} color="#fff" />
        </Pressable>
        <Text style={styles.headerTitle}>Agregar Producto</Text>
        <View style={styles.headerSpacer} />
      </View>

      <ScrollView
        contentContainerStyle={styles.content}
        keyboardShouldPersistTaps="handled"
        showsVerticalScrollIndicator={false}
      >
        {/* ── Product info ── */}
        <Text style={styles.sectionTitle}>Información del producto</Text>

        <TextInputField
          label="Nombre del producto"
          placeholder="Ej. Gaseosa 600ml"
          value={name}
          onChangeText={t => { setName(t); clearError('name'); }}
          error={errors.name}
          returnKeyType="next"
        />

        <SelectField
          label="Categoría"
          value={category}
          options={PRODUCT_CATEGORIES}
          placeholder="Selecciona una categoría"
          onSelect={v => { setCategory(v); clearError('category'); }}
          error={errors.category}
        />

        <TextInputField
          label="Precio de venta"
          placeholder="0"
          value={price}
          onChangeText={t => { setPrice(t.replace(/[^0-9]/g, '')); clearError('price'); }}
          keyboardType="numeric"
          error={errors.price}
          returnKeyType="next"
        />

        <TextInputField
          label="Stock inicial"
          placeholder="0"
          value={stock}
          onChangeText={t => { setStock(t.replace(/[^0-9]/g, '')); clearError('stock'); }}
          keyboardType="numeric"
          error={errors.stock}
          returnKeyType="done"
        />

        <SelectField
          label="Unidad de medida"
          value={unit}
          options={PRODUCT_UNITS}
          placeholder="Selecciona unidad"
          onSelect={v => { setUnit(v); clearError('unit'); }}
          error={errors.unit}
        />

        {/* ── Barcode ── */}
        <Text style={[styles.sectionTitle, styles.sectionGap]}>Código de barras</Text>

        <View style={styles.barcodeRow}>
          <View style={styles.barcodeInputWrap}>
            <TextInputField
              placeholder="Ingresa o escanea el código"
              value={barcode}
              onChangeText={t => { setBarcode(t); clearError('barcode'); }}
              keyboardType="number-pad"
              returnKeyType="done"
              error={errors.barcode}
            />
          </View>
          <Pressable
            onPress={() => setScannerOpen(true)}
            style={({ pressed }) => [styles.scanBtn, pressed && styles.scanBtnPressed]}
            accessibilityLabel="Escanear código de barras"
          >
            <IconSymbol name="barcode.viewfinder" size={26} color="#fff" />
          </Pressable>
        </View>

        {/* ── Image ── */}
        <Text style={[styles.sectionTitle, styles.sectionGap]}>
          Imagen del producto (opcional)
        </Text>

        <Pressable onPress={showImageOptions} style={styles.imagePicker} disabled={imageProcessing}>
          {imageProcessing ? (
            /* Processing state */
            <View style={styles.imageProcessingContent}>
              <ActivityIndicator size="large" color={Brand.orange} />
              <Text style={styles.imageProcessingText}>Removiendo fondo...</Text>
            </View>
          ) : imageUri ? (
            /* Image selected */
            <View style={styles.imagePreviewContent}>
              {/* White background so the transparent PNG looks clean */}
              <View style={styles.imageWhiteBg}>
                <Image
                  source={{ uri: imageUri }}
                  style={styles.imagePreview}
                  resizeMode="contain"
                />
              </View>
              <View style={styles.imageEditRow}>
                <IconSymbol name="camera.fill" size={16} color={Brand.orange} />
                <Text style={styles.imageEditText}>Cambiar imagen</Text>
              </View>
            </View>
          ) : (
            /* Empty state */
            <>
              <IconSymbol name="camera.fill" size={32} color="#B0A090" />
              <Text style={styles.imagePickerText}>Tomar foto o seleccionar imagen</Text>
              <Text style={styles.imagePickerSub}>
                El fondo se removerá automáticamente ✨
              </Text>
            </>
          )}
        </Pressable>

        <View style={styles.saveBtn}>
          <AppButton label="Guardar producto" onPress={handleSave} loading={saving} />
        </View>
      </ScrollView>

      {/* Barcode scanner modal */}
      <BarcodeScannerModal
        visible={scannerOpen}
        onScan={code => { setBarcode(code); clearError('barcode'); }}
        onClose={() => setScannerOpen(false)}
      />
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  flex: { flex: 1, backgroundColor: Brand.cream },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 16,
    paddingBottom: 16,
    backgroundColor: Brand.brown,
    gap: 12,
  },
  headerTitle: {
    flex: 1,
    fontSize: 20,
    fontWeight: '700',
    color: '#fff',
    textAlign: 'center',
  },
  headerSpacer: { width: 24 },
  content: { padding: 20, paddingBottom: 48 },
  sectionTitle: {
    fontSize: 13,
    fontWeight: '700',
    color: Brand.orange,
    marginBottom: 14,
    textTransform: 'uppercase',
    letterSpacing: 0.6,
  },
  sectionGap: { marginTop: 6 },

  // Barcode
  barcodeRow: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    gap: 10,
    marginBottom: 4,
  },
  barcodeInputWrap: { flex: 1 },
  scanBtn: {
    width: 50,
    height: 50,
    borderRadius: 10,
    backgroundColor: Brand.orange,
    alignItems: 'center',
    justifyContent: 'center',
  },
  scanBtnPressed: { opacity: 0.75 },

  // Image picker
  imagePicker: {
    borderWidth: 1.5,
    borderColor: '#E8DDD0',
    borderStyle: 'dashed',
    borderRadius: 12,
    backgroundColor: '#fff',
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: 24,
    gap: 8,
    marginBottom: 8,
    minHeight: 140,
  },
  imagePickerText: { fontSize: 14, color: '#8A7060' },
  imagePickerSub: { fontSize: 12, color: Brand.orange, fontWeight: '500' },

  // Processing state
  imageProcessingContent: { alignItems: 'center', gap: 12 },
  imageProcessingText: { fontSize: 14, color: '#8A7060' },

  // Preview state
  imagePreviewContent: { width: '100%', alignItems: 'center', gap: 10 },
  imageWhiteBg: {
    width: '100%',
    height: 160,
    backgroundColor: '#fff',
    borderRadius: 8,
    alignItems: 'center',
    justifyContent: 'center',
    overflow: 'hidden',
  },
  imagePreview: { width: '90%', height: 150 },
  imageEditRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
  },
  imageEditText: { fontSize: 13, color: Brand.orange, fontWeight: '500' },

  saveBtn: { marginTop: 24 },
});

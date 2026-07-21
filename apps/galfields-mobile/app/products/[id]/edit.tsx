import { useEffect, useState } from 'react';
import {
  ActivityIndicator,
  Alert,
  KeyboardAvoidingView,
  Platform,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import { router, useLocalSearchParams } from 'expo-router';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { AppButton } from '@/components/ui/app-button';
import { TextInputField } from '@/components/ui/text-input-field';
import { SelectField } from '@/components/ui/select-field';
import { IconSymbol } from '@/components/ui/icon-symbol';
import { BarcodeScannerModal } from '@/components/products/barcode-scanner-modal';
import { ImagePickerField } from '@/components/products/image-picker-field';
import { VariantFormCard, type VariantFormErrors } from '@/components/products/variant-form-card';
import { useImagePicker } from '@/hooks/use-image-picker';
import { useProducts } from '@/contexts/products-context';
import { brandsApi, categoriesApi, type CatalogBrand, type CatalogCategory } from '@/services/catalog-api';
import { fetchProduct, toVariantDraft } from '@/services/products-api';
import { Brand } from '@/constants/theme';
import { useThemeColors } from '@/hooks/use-theme-colors';
import { toTitleCase } from '@/utils/text-case';
import { buildVariantSku } from '@/utils/sku';
import { createEmptyVariantDraft, type ProductInput, type ProductVariantDraft } from '@/types/product';

interface FormErrors {
  name?: string;
  category?: string;
  brand?: string;
  variants: VariantFormErrors[];
}

export default function EditProductScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const { updateProduct, removeProduct } = useProducts();
  const insets = useSafeAreaInsets();
  const colors = useThemeColors();

  const [loadingProduct, setLoadingProduct] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);

  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [categoryName, setCategoryName] = useState('');
  const [brandName, setBrandName] = useState('');
  const [imageUri, setImageUri] = useState<string | undefined>(undefined);
  const [variants, setVariants] = useState<ProductVariantDraft[]>([createEmptyVariantDraft()]);
  const [scanningIndex, setScanningIndex] = useState<number | null>(null);
  const [errors, setErrors] = useState<FormErrors>({ variants: [{}] });
  const [saving, setSaving] = useState(false);
  const [deleting, setDeleting] = useState(false);

  const [categories, setCategories] = useState<CatalogCategory[]>([]);
  const [brands, setBrands] = useState<CatalogBrand[]>([]);
  const [catalogError, setCatalogError] = useState<string | null>(null);

  useEffect(() => {
    Promise.all([categoriesApi.list(), brandsApi.list()])
      .then(([categoryList, brandList]) => {
        setCategories(categoryList);
        setBrands(brandList);
      })
      .catch(err => {
        setCatalogError(
          err instanceof Error ? err.message : 'No se pudieron cargar categorías y marcas.',
        );
      });
  }, []);

  useEffect(() => {
    fetchProduct(id)
      .then(product => {
        setName(product.name);
        setDescription(product.description ?? '');
        setCategoryName(product.categoryName ?? '');
        setBrandName(product.brandName ?? '');
        setImageUri(product.imageUrl ?? undefined);
        const drafts = product.variants.map(toVariantDraft);
        setVariants(drafts.length > 0 ? drafts : [createEmptyVariantDraft()]);
        setErrors({ variants: drafts.length > 0 ? drafts.map(() => ({})) : [{}] });
      })
      .catch(err => {
        setLoadError(err instanceof Error ? err.message : 'No se pudo cargar el producto.');
      })
      .finally(() => setLoadingProduct(false));
  }, [id]);

  const mainImagePicker = useImagePicker(uri => setImageUri(uri ?? undefined));

  const clearError = (field: 'name' | 'category' | 'brand') =>
    setErrors(prev => ({ ...prev, [field]: undefined }));

  const updateVariant = (index: number, next: ProductVariantDraft) => {
    setVariants(prev => prev.map((v, i) => (i === index ? next : v)));
    setErrors(prev => ({
      ...prev,
      variants: prev.variants.map((e, i) => (i === index ? {} : e)),
    }));
  };

  const addVariant = () => {
    setVariants(prev => [...prev, createEmptyVariantDraft()]);
    setErrors(prev => ({ ...prev, variants: [...prev.variants, {}] }));
  };

  const removeVariant = (index: number) => {
    setVariants(prev => prev.filter((_, i) => i !== index));
    setErrors(prev => ({ ...prev, variants: prev.variants.filter((_, i) => i !== index) }));
  };

  const isVariantComplete = (variant: ProductVariantDraft): boolean =>
    variant.barcode.trim().length > 0 &&
    variant.price.trim().length > 0 &&
    Number(variant.price) > 0 &&
    variant.costPrice.trim().length > 0 &&
    Number(variant.costPrice) >= 0 &&
    (!variant.initialStock.trim() || Number(variant.initialStock) >= 0) &&
    variant.attributes.some(a => a.name.trim() && a.value.trim());

  const canSave =
    name.trim().length > 0 && !!categoryName && !!brandName && variants.every(isVariantComplete);

  const validate = (): boolean => {
    const variantErrors: VariantFormErrors[] = variants.map(variant => {
      const fieldErrors: VariantFormErrors = {};
      if (!variant.barcode.trim()) fieldErrors.barcode = 'El código de barras es requerido';
      if (!variant.price.trim() || Number(variant.price) <= 0)
        fieldErrors.price = 'Ingresa un precio válido';
      if (!variant.costPrice.trim() || Number(variant.costPrice) < 0)
        fieldErrors.costPrice = 'Ingresa un costo válido';
      if (variant.initialStock.trim() && Number(variant.initialStock) < 0)
        fieldErrors.initialStock = 'Ingresa un stock válido';
      if (!variant.attributes.some(a => a.name.trim() && a.value.trim()))
        fieldErrors.attributes = 'Agrega al menos un atributo (ej. tamaño, color)';
      return fieldErrors;
    });

    const next: FormErrors = {
      variants: variantErrors,
      name: name.trim() ? undefined : 'El nombre es requerido',
      category: categoryName ? undefined : 'Selecciona una categoría',
      brand: brandName ? undefined : 'Selecciona una marca',
    };

    setErrors(next);
    const hasVariantErrors = variantErrors.some(e => Object.keys(e).length > 0);
    return !next.name && !next.category && !next.brand && !hasVariantErrors;
  };

  const handleSave = async () => {
    if (!validate()) return;

    const category = categories.find(c => c.name === categoryName);
    const brand = brands.find(b => b.name === brandName);
    if (!category || !brand) {
      Alert.alert('Error', 'Selecciona una categoría y marca válidas.', [{ text: 'OK' }]);
      return;
    }

    setSaving(true);
    try {
      const payload: ProductInput = {
        name: name.trim(),
        description: description.trim(),
        categoryId: category.id,
        brandId: brand.id,
        imageUri,
        variants: variants.map(v => {
          const barcode = v.barcode.trim();
          const filteredAttributes = v.attributes.filter(a => a.name.trim() && a.value.trim());
          return {
            sku: v.originalSku ?? buildVariantSku(name.trim(), filteredAttributes[0]?.value ?? '', barcode),
            barcode,
            price: Number(v.price),
            costPrice: Number(v.costPrice),
            initialStock: v.initialStock.trim() ? Number(v.initialStock) : 0,
            attributes: filteredAttributes,
            imageUri: v.imageUri,
          };
        }),
      };

      await updateProduct(id, payload);
      Alert.alert('Producto actualizado', 'Los cambios se guardaron correctamente.', [
        { text: 'OK', onPress: () => router.back() },
      ]);
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'Error desconocido';
      Alert.alert('No se pudo guardar el producto', msg, [{ text: 'OK' }]);
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = () => {
    Alert.alert(
      'Eliminar producto',
      `¿Eliminar "${name}"? Ya no aparecerá disponible para venta ni en el catálogo.`,
      [
        { text: 'Cancelar', style: 'cancel' },
        {
          text: 'Eliminar',
          style: 'destructive',
          onPress: async () => {
            setDeleting(true);
            try {
              await removeProduct(id);
              router.back();
            } catch (err) {
              const msg = err instanceof Error ? err.message : 'Error desconocido';
              Alert.alert('No se pudo eliminar el producto', msg, [{ text: 'OK' }]);
              setDeleting(false);
            }
          },
        },
      ],
    );
  };

  if (loadingProduct) {
    return (
      <View style={[styles.flex, styles.centered, { backgroundColor: colors.background }]}>
        <ActivityIndicator size="large" color={Brand.orange} />
      </View>
    );
  }

  if (loadError) {
    return (
      <View style={[styles.flex, styles.centered, { backgroundColor: colors.background }]}>
        <Text style={styles.errorBannerText}>{loadError}</Text>
        <Pressable onPress={() => router.back()} style={styles.backLink}>
          <Text style={styles.backLinkText}>Volver</Text>
        </Pressable>
      </View>
    );
  }

  return (
    <KeyboardAvoidingView
      behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
      style={[styles.flex, { backgroundColor: colors.background }]}
    >
      {/* Header */}
      <View style={[styles.header, { paddingTop: insets.top + 8 }]}>
        <Pressable onPress={() => router.back()} hitSlop={10}>
          <IconSymbol name="arrow.left" size={24} color="#fff" />
        </Pressable>
        <Text style={styles.headerTitle}>Editar Producto</Text>
        <View style={styles.headerSpacer} />
      </View>

      {catalogError ? (
        <View style={styles.errorBanner}>
          <Text style={styles.errorBannerText}>{catalogError}</Text>
        </View>
      ) : null}

      <ScrollView
        contentContainerStyle={styles.content}
        keyboardShouldPersistTaps="handled"
        showsVerticalScrollIndicator={false}
      >
        {/* ── Product info ── */}
        <Text style={styles.sectionTitle}>Información del producto</Text>

        <TextInputField
          label="Nombre del producto"
          placeholder="Ej. Camiseta Básica"
          value={name}
          onChangeText={t => { setName(toTitleCase(t)); clearError('name'); }}
          error={errors.name}
          returnKeyType="next"
        />

        <TextInputField
          label="Descripción"
          placeholder="Ej. Camiseta de algodón"
          value={description}
          onChangeText={setDescription}
          multiline
          returnKeyType="next"
        />

        <SelectField
          label="Categoría"
          value={categoryName}
          options={categories.map(c => c.name)}
          placeholder="Selecciona una categoría"
          onSelect={v => { setCategoryName(v); clearError('category'); }}
          error={errors.category}
        />

        <SelectField
          label="Marca"
          value={brandName}
          options={brands.map(b => b.name)}
          placeholder="Selecciona una marca"
          onSelect={v => { setBrandName(v); clearError('brand'); }}
          error={errors.brand}
        />

        {/* ── Main image ── */}
        <Text style={[styles.sectionTitle, styles.sectionGap]}>
          Imagen del producto (opcional)
        </Text>
        <ImagePickerField
          imageUri={imageUri}
          processing={mainImagePicker.processing}
          searchQuery={name}
          onPick={mainImagePicker.pick}
          onPickFromSearch={mainImagePicker.pickFromUrl}
          onRemove={mainImagePicker.clear}
        />

        {/* ── Variants ── */}
        <Text style={[styles.sectionTitle, styles.sectionGap]}>Variantes</Text>
        {variants.map((variant, index) => (
          <VariantFormCard
            key={index}
            index={index}
            productName={name}
            value={variant}
            errors={errors.variants[index]}
            canRemove={variants.length > 1}
            onChange={next => updateVariant(index, next)}
            onRemove={() => removeVariant(index)}
            onScanBarcode={() => setScanningIndex(index)}
          />
        ))}

        <Pressable onPress={addVariant} style={styles.addVariantBtn}>
          <IconSymbol name="plus" size={18} color={Brand.orange} />
          <Text style={styles.addVariantText}>Agregar variante</Text>
        </Pressable>

        <View style={styles.saveBtn}>
          <AppButton
            label="Guardar cambios"
            onPress={handleSave}
            loading={saving}
            disabled={!canSave}
          />
        </View>

        <Pressable
          onPress={handleDelete}
          disabled={saving || deleting}
          style={({ pressed }) => [styles.deleteBtn, (pressed || deleting) && styles.deleteBtnPressed]}
        >
          {deleting ? (
            <ActivityIndicator color={Brand.danger} />
          ) : (
            <>
              <IconSymbol name="trash.fill" size={18} color={Brand.danger} />
              <Text style={styles.deleteBtnText}>Eliminar producto</Text>
            </>
          )}
        </Pressable>
      </ScrollView>

      {/* Barcode scanner modal, shared across variants */}
      <BarcodeScannerModal
        visible={scanningIndex !== null}
        onScan={code => {
          if (scanningIndex === null) return;
          updateVariant(scanningIndex, { ...variants[scanningIndex], barcode: code });
        }}
        onClose={() => setScanningIndex(null)}
      />
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  flex: { flex: 1 },
  centered: { alignItems: 'center', justifyContent: 'center', gap: 12, padding: 20 },
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
  errorBanner: {
    backgroundColor: `${Brand.danger}14`,
    paddingVertical: 10,
    paddingHorizontal: 16,
  },
  errorBannerText: { color: Brand.danger, fontSize: 13, textAlign: 'center' },
  backLink: { marginTop: 4 },
  backLinkText: { color: Brand.orange, fontSize: 14, fontWeight: '600' },
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

  addVariantBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 8,
    paddingVertical: 14,
    borderWidth: 1.5,
    borderColor: Brand.orange,
    borderStyle: 'dashed',
    borderRadius: 12,
    marginBottom: 8,
  },
  addVariantText: { fontSize: 14, fontWeight: '600', color: Brand.orange },

  saveBtn: { marginTop: 24 },

  deleteBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 8,
    marginTop: 14,
    paddingVertical: 14,
    borderRadius: 12,
  },
  deleteBtnPressed: { opacity: 0.6 },
  deleteBtnText: { fontSize: 14, fontWeight: '600', color: Brand.danger },
});

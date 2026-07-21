import { useState } from 'react';
import { ActivityIndicator, Alert, Image, Pressable, StyleSheet, Text, View } from 'react-native';
import { IconSymbol } from '@/components/ui/icon-symbol';
import { ImageSearchModal } from '@/components/products/image-search-modal';
import { Brand } from '@/constants/theme';
import { useThemeColors } from '@/hooks/use-theme-colors';

interface ImagePickerFieldProps {
  imageUri?: string | null;
  processing: boolean;
  onPick: (source: 'camera' | 'gallery') => void;
  /** Omit to drop the "Buscar en internet" option entirely — non-product
   * callers (e.g. payment methods, via `usePlainImagePicker`) have no
   * `pickFromUrl` to wire it to. */
  onPickFromSearch?: (fullUrl: string, thumbnailUrl: string) => void;
  onRemove: () => void;
  /** Prefills `ImageSearchModal`'s query — the product/variant name, most of
   * the time. Defaults to an empty search box when there's nothing to
   * suggest yet (e.g. the name field is still blank). */
  searchQuery?: string;
  compact?: boolean;
}

export function ImagePickerField({
  imageUri,
  processing,
  onPick,
  onPickFromSearch,
  onRemove,
  searchQuery = '',
  compact,
}: ImagePickerFieldProps) {
  const colors = useThemeColors();
  const [searchVisible, setSearchVisible] = useState(false);

  const showOptions = () => {
    Alert.alert('Imagen', 'Elige una opción', [
      { text: 'Tomar foto', onPress: () => onPick('camera') },
      { text: 'Seleccionar de galería', onPress: () => onPick('gallery') },
      ...(onPickFromSearch
        ? [{ text: 'Buscar en internet', onPress: () => setSearchVisible(true) }]
        : []),
      ...(imageUri
        ? [{ text: 'Eliminar imagen', style: 'destructive' as const, onPress: onRemove }]
        : []),
      { text: 'Cancelar', style: 'cancel' as const },
    ]);
  };

  return (
    <>
      <Pressable
        onPress={showOptions}
        disabled={processing}
        style={[
          styles.picker,
          { borderColor: colors.border, backgroundColor: colors.card },
          compact && styles.pickerCompact,
        ]}
      >
        {processing ? (
          <View style={styles.processingContent}>
            <ActivityIndicator size={compact ? 'small' : 'large'} color={Brand.orange} />
            {!compact && (
              <Text style={[styles.processingText, { color: colors.textSecondary }]}>Descargando imagen...</Text>
            )}
          </View>
        ) : imageUri ? (
          <View style={styles.previewContent}>
            <View style={[styles.whiteBg, compact && styles.whiteBgCompact]}>
              <Image source={{ uri: imageUri }} style={styles.previewImage} resizeMode="contain" />
            </View>
            {!compact && (
              <View style={styles.editRow}>
                <IconSymbol name="camera.fill" size={16} color={Brand.orange} />
                <Text style={styles.editText}>Cambiar imagen</Text>
              </View>
            )}
          </View>
        ) : (
          <>
            <IconSymbol name="camera.fill" size={compact ? 22 : 32} color={colors.placeholder} />
            {!compact && (
              <Text style={[styles.pickerText, { color: colors.textSecondary }]}>
                Tomar foto, seleccionar imagen o buscar en internet
              </Text>
            )}
          </>
        )}
      </Pressable>

      <ImageSearchModal
        visible={searchVisible}
        initialQuery={searchQuery}
        onSelect={(fullUrl, thumbnailUrl) => {
          setSearchVisible(false);
          onPickFromSearch?.(fullUrl, thumbnailUrl);
        }}
        onClose={() => setSearchVisible(false)}
      />
    </>
  );
}

const styles = StyleSheet.create({
  picker: {
    borderWidth: 1.5,
    borderStyle: 'dashed',
    borderRadius: 12,
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: 24,
    gap: 8,
    minHeight: 140,
  },
  pickerCompact: {
    paddingVertical: 8,
    minHeight: 64,
    width: 64,
  },
  pickerText: { fontSize: 14, textAlign: 'center' },

  processingContent: { alignItems: 'center', gap: 12 },
  processingText: { fontSize: 14 },

  previewContent: { width: '100%', alignItems: 'center', gap: 10 },
  whiteBg: {
    width: '100%',
    height: 160,
    backgroundColor: '#fff',
    borderRadius: 8,
    alignItems: 'center',
    justifyContent: 'center',
    overflow: 'hidden',
  },
  whiteBgCompact: { height: 60, width: 60 },
  previewImage: { width: '90%', height: '90%' },
  editRow: { flexDirection: 'row', alignItems: 'center', gap: 6 },
  editText: { fontSize: 13, color: Brand.orange, fontWeight: '500' },
});

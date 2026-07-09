import { ActivityIndicator, Alert, Image, Pressable, StyleSheet, Text, View } from 'react-native';
import { IconSymbol } from '@/components/ui/icon-symbol';
import { Brand } from '@/constants/theme';

interface ImagePickerFieldProps {
  imageUri?: string | null;
  processing: boolean;
  onPick: (source: 'camera' | 'gallery') => void;
  onRemove: () => void;
  compact?: boolean;
}

export function ImagePickerField({ imageUri, processing, onPick, onRemove, compact }: ImagePickerFieldProps) {
  const showOptions = () => {
    Alert.alert('Imagen', 'Elige una opción', [
      { text: 'Tomar foto', onPress: () => onPick('camera') },
      { text: 'Seleccionar de galería', onPress: () => onPick('gallery') },
      ...(imageUri
        ? [{ text: 'Eliminar imagen', style: 'destructive' as const, onPress: onRemove }]
        : []),
      { text: 'Cancelar', style: 'cancel' as const },
    ]);
  };

  return (
    <Pressable
      onPress={showOptions}
      disabled={processing}
      style={[styles.picker, compact && styles.pickerCompact]}
    >
      {processing ? (
        <View style={styles.processingContent}>
          <ActivityIndicator size={compact ? 'small' : 'large'} color={Brand.orange} />
          {!compact && <Text style={styles.processingText}>Removiendo fondo...</Text>}
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
          <IconSymbol name="camera.fill" size={compact ? 22 : 32} color="#B0A090" />
          {!compact && <Text style={styles.pickerText}>Tomar foto o seleccionar imagen</Text>}
        </>
      )}
    </Pressable>
  );
}

const styles = StyleSheet.create({
  picker: {
    borderWidth: 1.5,
    borderColor: '#E8DDD0',
    borderStyle: 'dashed',
    borderRadius: 12,
    backgroundColor: '#fff',
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
  pickerText: { fontSize: 14, color: '#8A7060', textAlign: 'center' },

  processingContent: { alignItems: 'center', gap: 12 },
  processingText: { fontSize: 14, color: '#8A7060' },

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

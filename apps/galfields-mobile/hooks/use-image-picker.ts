import { useState } from 'react';
import { Alert } from 'react-native';
import * as ImagePicker from 'expo-image-picker';
import { removeBackground } from '@/services/background-removal';

/**
 * Picks a photo (camera or gallery), removes its background, and reports the
 * result back to the caller. Controlled from outside — the caller owns where
 * the resulting uri lives (main product image, or one variant's image) and
 * just passes an onChange callback; this hook only owns the transient
 * "processing" state for the background-removal call.
 */
export function useImagePicker(onChange: (uri: string | null) => void) {
  const [processing, setProcessing] = useState(false);

  const pick = async (source: 'camera' | 'gallery') => {
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
    setProcessing(true);
    onChange(null);
    try {
      const processedUri = await removeBackground(uri);
      onChange(processedUri);
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'Error desconocido';
      Alert.alert(
        'No se pudo remover el fondo',
        `${msg}\n\nSe usará la imagen original.`,
        [{ text: 'OK' }],
      );
      onChange(uri);
    } finally {
      setProcessing(false);
    }
  };

  const clear = () => onChange(null);

  return { processing, pick, clear };
}

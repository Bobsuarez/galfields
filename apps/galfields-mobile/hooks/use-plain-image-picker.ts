import { Alert } from 'react-native';
import * as ImagePicker from 'expo-image-picker';

/**
 * Picks a photo (camera or gallery) and reports its uri back untouched.
 * Unlike useImagePicker, this has no `pickFromUrl` for image-search results
 * — that's a product-only flow (see components/products/image-search-modal.tsx);
 * other entities that just need a plain uploaded image (e.g. payment methods)
 * use this.
 */
export function usePlainImagePicker(onChange: (uri: string | null) => void) {
  const pick = async (source: 'camera' | 'gallery') => {
    // launchCameraAsync/launchImageLibraryAsync don't request permission
    // themselves (unlike expo-camera's CameraView) - without this, they
    // reject outright with "Missing camera or camera roll permission"
    // instead of ever showing the OS prompt.
    if (source === 'camera') {
      const { granted } = await ImagePicker.requestCameraPermissionsAsync();
      if (!granted) {
        Alert.alert('Permiso requerido', 'Necesitamos acceso a la cámara para tomar fotos.');
        return;
      }
    } else {
      const { granted } = await ImagePicker.requestMediaLibraryPermissionsAsync();
      if (!granted) {
        Alert.alert('Permiso requerido', 'Necesitamos acceso a tus fotos para seleccionar una imagen.');
        return;
      }
    }

    const result =
      source === 'camera'
        ? await ImagePicker.launchCameraAsync({ mediaTypes: ['images'], quality: 0.85, allowsEditing: false })
        : await ImagePicker.launchImageLibraryAsync({ mediaTypes: ['images'], quality: 0.85, allowsEditing: false });

    if (result.canceled) return;
    onChange(result.assets[0].uri);
  };

  const clear = () => onChange(null);

  return { pick, clear };
}

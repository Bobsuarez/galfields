import * as ImagePicker from 'expo-image-picker';

/**
 * Picks a photo (camera or gallery) and reports its uri back untouched.
 * Unlike useImagePicker, this does not run background removal — that's a
 * product-only step (see services/background-removal.ts); other entities
 * that just need a plain uploaded image (e.g. payment methods) use this.
 */
export function usePlainImagePicker(onChange: (uri: string | null) => void) {
  const pick = async (source: 'camera' | 'gallery') => {
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

import { useState } from 'react';
import { Alert } from 'react-native';
import * as ImagePicker from 'expo-image-picker';
import * as FileSystem from 'expo-file-system/legacy';
import { guessImageMimeType } from '@/utils/image-mime';

/** Downloads a remote image (a Google Custom Search result) to a local
 * cache file — the multipart upload in `services/products-api.ts` needs a
 * local uri (see that file's `isLocalImageUri`), not a remote https url. */
async function downloadToCache(url: string): Promise<string> {
  const mimeType = guessImageMimeType(url);
  const extension = mimeType === 'image/png' ? 'png' : mimeType === 'image/webp' ? 'webp' : 'jpg';
  const dest = `${FileSystem.cacheDirectory}search_${Date.now()}.${extension}`;
  const { uri } = await FileSystem.downloadAsync(url, dest);
  return uri;
}

/**
 * Picks a product photo — camera, gallery, or (via `pickFromUrl`) a result
 * chosen from `ImageSearchModal` — and reports its local uri back to the
 * caller. Controlled from outside — the caller owns where the resulting uri
 * lives (main product image, or one variant's image) and just passes an
 * onChange callback; this hook only owns the transient "processing" state
 * while a search result downloads.
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
    onChange(result.assets[0].uri);
  };

  /**
   * Counterpart to `pick`, for a picture chosen from `ImageSearchModal`.
   * `url` is the search result's full-size image — its host may reject the
   * download (hotlink protection, since it's an arbitrary site out on the
   * web), so `fallbackUrl` (the result's Google-hosted thumbnail) is tried
   * next before giving up entirely.
   */
  const pickFromUrl = async (url: string, fallbackUrl?: string) => {
    setProcessing(true);
    onChange(null);
    try {
      let localUri: string;
      try {
        localUri = await downloadToCache(url);
      } catch (err) {
        if (!fallbackUrl) throw err;
        localUri = await downloadToCache(fallbackUrl);
      }
      onChange(localUri);
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'Error desconocido';
      Alert.alert('No se pudo descargar la imagen', msg, [{ text: 'OK' }]);
    } finally {
      setProcessing(false);
    }
  };

  const clear = () => onChange(null);

  return { processing, pick, pickFromUrl, clear };
}

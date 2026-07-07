import * as FileSystem from 'expo-file-system/legacy';

const CLIPDROP_URL = 'https://clipdrop-api.co/remove-background/v1';

function arrayBufferToBase64(buffer: ArrayBuffer): string {
  const bytes = new Uint8Array(buffer);
  let binary = '';
  // Process in chunks to avoid call-stack overflow on large images
  const CHUNK = 8192;
  for (let i = 0; i < bytes.byteLength; i += CHUNK) {
    binary += String.fromCharCode(...(bytes.subarray(i, i + CHUNK) as unknown as number[]));
  }
  return btoa(binary);
}

/**
 * Sends an image to the Clipdrop API and returns a local URI pointing to
 * the result PNG (transparent background). Throws on network or API errors.
 */
export async function removeBackground(imageUri: string): Promise<string> {
  const apiKey = process.env.EXPO_PUBLIC_CLIPDROP_API_KEY;
  if (!apiKey) throw new Error('EXPO_PUBLIC_CLIPDROP_API_KEY no está configurada.');

  const formData = new FormData();
  formData.append('image_file', {
    uri: imageUri,
    name: 'product.jpg',
    type: 'image/jpeg',
  } as unknown as Blob);

  const response = await fetch(CLIPDROP_URL, {
    method: 'POST',
    headers: { 'x-api-key': apiKey },
    body: formData,
  });

  if (!response.ok) {
    const text = await response.text().catch(() => response.status.toString());
    throw new Error(`Clipdrop ${response.status}: ${text}`);
  }

  const arrayBuffer = await response.arrayBuffer();
  const base64 = arrayBufferToBase64(arrayBuffer);

  const outputPath = `${FileSystem.cacheDirectory}product_nobg_${Date.now()}.png`;
  await FileSystem.writeAsStringAsync(outputPath, base64, {
    encoding: FileSystem.EncodingType.Base64,
  });

  return outputPath;
}

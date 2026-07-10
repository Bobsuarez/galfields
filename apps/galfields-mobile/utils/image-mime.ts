export type ImageMimeType = 'image/png' | 'image/webp' | 'image/jpeg';

const EXTENSION_BY_MIME: Record<ImageMimeType, string> = {
  'image/png': 'png',
  'image/webp': 'webp',
  'image/jpeg': 'jpg',
};

/** Guesses the mime type of a local image file from its uri's extension —
 * expo-image-picker/camera results don't carry a reliable mime type of
 * their own, so this is what every image upload (products, variant photos,
 * background removal) bases its `type`/filename on. */
export function guessImageMimeType(uri: string): ImageMimeType {
  const extension = uri.split('.').pop()?.toLowerCase();
  if (extension === 'png') return 'image/png';
  if (extension === 'webp') return 'image/webp';
  return 'image/jpeg';
}

/** Filename to send in a multipart file part, with an extension that
 * actually matches `mimeType` — mismatching the two (e.g. naming a webp
 * file "x.jpg") is what caused the backend to store the wrong Content-Type
 * for webp uploads. */
export function imageFileName(fieldName: string, mimeType: ImageMimeType): string {
  return `${fieldName}.${EXTENSION_BY_MIME[mimeType]}`;
}

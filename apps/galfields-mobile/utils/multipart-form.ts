import * as FileSystem from 'expo-file-system/legacy';
import { guessImageMimeType, imageFileName } from './image-mime';

/**
 * React Native's `Blob` polyfill isn't reliable for in-memory JSON parts
 * (the multipart body it produces can arrive empty/malformed on-device even
 * though the exact same `Blob`+`FormData` code works fine under a
 * spec-compliant fetch implementation) — write the JSON to a temp file
 * instead and attach it the same proven way images already are, as a
 * `{ uri, name, type }` file part.
 */
export async function jsonPart(
  fieldName: string,
  value: unknown,
): Promise<{ uri: string; name: string; type: string }> {
  const path = `${FileSystem.cacheDirectory}${fieldName}_${Date.now()}.json`;
  await FileSystem.writeAsStringAsync(path, JSON.stringify(value));
  return { uri: path, name: `${fieldName}.json`, type: 'application/json' };
}

export function appendImagePart(formData: FormData, fieldName: string, uri: string): void {
  const mimeType = guessImageMimeType(uri);
  formData.append(fieldName, {
    uri,
    name: imageFileName(fieldName, mimeType),
    type: mimeType,
  } as unknown as Blob);
}

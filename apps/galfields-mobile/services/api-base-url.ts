export function apiBaseUrl(): string {
  const url = process.env.EXPO_PUBLIC_API_BASE_URL;
  if (!url) throw new Error('EXPO_PUBLIC_API_BASE_URL no está configurada.');
  return url;
}

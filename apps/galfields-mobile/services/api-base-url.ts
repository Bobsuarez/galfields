import AsyncStorage from '@react-native-async-storage/async-storage';

const STORAGE_KEY = 'apiBaseUrl';

/** In-memory cache, populated once by `initApiBaseUrl()` at app boot (see
 * `app/_layout.tsx`) — `apiBaseUrl()` itself stays synchronous, since every
 * `services/*-api.ts` file calls it inline inside a template literal, and
 * an async AsyncStorage read can't happen there. */
let cachedBaseUrl: string | null = null;

function envDefault(): string | undefined {
  return process.env.EXPO_PUBLIC_API_BASE_URL;
}

/** Loads the stored override (if the user ever set one from Configuración
 * → Servidor) or falls back to `EXPO_PUBLIC_API_BASE_URL` — call once at
 * app boot, before any screen that might call `apiBaseUrl()` can render. */
export async function initApiBaseUrl(): Promise<void> {
  const stored = await AsyncStorage.getItem(STORAGE_KEY);
  cachedBaseUrl = stored || envDefault() || null;
}

/** Synchronous — every `services/*-api.ts` file calls this inline. Throws
 * if `initApiBaseUrl()` hasn't run yet or found nothing, same defensive
 * style as before this was configurable (should be unreachable in
 * practice — see `app/_layout.tsx`). */
export function apiBaseUrl(): string {
  if (!cachedBaseUrl) throw new Error('La URL del servidor no está configurada.');
  return cachedBaseUrl;
}

/** Current value without throwing — for prefilling the Configuración →
 * Servidor screen. */
export function currentApiBaseUrl(): string | null {
  return cachedBaseUrl;
}

/** Persists a new server URL and updates the in-memory cache immediately —
 * every subsequent `apiBaseUrl()` call (and therefore every API call after
 * this resolves) uses it right away, no app restart needed. */
export async function setApiBaseUrl(url: string): Promise<void> {
  const trimmed = url.trim().replace(/\/+$/, '');
  if (!/^https?:\/\/.+/.test(trimmed)) {
    throw new Error('La URL debe empezar con http:// o https://');
  }
  await AsyncStorage.setItem(STORAGE_KEY, trimmed);
  cachedBaseUrl = trimmed;
}

/** Clears the stored override, reverting to `EXPO_PUBLIC_API_BASE_URL`. */
export async function resetApiBaseUrl(): Promise<void> {
  await AsyncStorage.removeItem(STORAGE_KEY);
  cachedBaseUrl = envDefault() ?? null;
}

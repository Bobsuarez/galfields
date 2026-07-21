import AsyncStorage from '@react-native-async-storage/async-storage';

export type ImageSearchProvider = 'google' | 'serpapi';

const STORAGE_KEY = 'imageSearchProvider';
const DEFAULT_PROVIDER: ImageSearchProvider = 'google';

/** In-memory cache, populated once by `initImageSearchProvider()` at app
 * boot (see `app/_layout.tsx`) — same "sync read, async init" split as
 * `services/api-base-url.ts`, since `services/image-search.ts` needs to
 * pick a provider synchronously mid-call. */
let cachedProvider: ImageSearchProvider = DEFAULT_PROVIDER;

/** Loads the stored choice (if the user ever set one from Configuración →
 * Búsqueda de imágenes) — call once at app boot, before any screen that
 * might trigger an image search can render. */
export async function initImageSearchProvider(): Promise<void> {
  const stored = await AsyncStorage.getItem(STORAGE_KEY);
  cachedProvider = stored === 'serpapi' ? 'serpapi' : DEFAULT_PROVIDER;
}

/** Synchronous — `services/image-search.ts` calls this to pick which API
 * to hit. Defaults to `'google'` until `initImageSearchProvider()` runs. */
export function imageSearchProvider(): ImageSearchProvider {
  return cachedProvider;
}

/** Persists the choice and updates the in-memory cache immediately — the
 * very next search uses it, no app restart needed. */
export async function setImageSearchProvider(provider: ImageSearchProvider): Promise<void> {
  await AsyncStorage.setItem(STORAGE_KEY, provider);
  cachedProvider = provider;
}

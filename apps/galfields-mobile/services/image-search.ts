import { imageSearchProvider } from './image-search-provider';

export interface ImageSearchResult {
  /** Provider-hosted thumbnail (Google's encrypted-tbn0.gstatic.com, or
   * SerpApi's own cache) — small, fast, and reliable to display in the
   * picker grid regardless of the source site. */
  thumbnailUrl: string;
  /** Original full-size image on its source site — used for the final
   * product photo, but its host may block hotlinking/downloading (see
   * `hooks/use-image-picker.ts`'s fallback to `thumbnailUrl` on failure). */
  fullUrl: string;
  title: string;
}

/**
 * Looks up product images, biased towards plain/white backgrounds so
 * results drop straight into a product photo without much cleanup, capped
 * at 10 (the picker's "first 10 results"). Dispatches to whichever
 * provider is selected in Configuración → Búsqueda de imágenes (see
 * `services/image-search-provider.ts`) — added as a switchable fallback
 * while Google Custom Search JSON API access issues get sorted out on the
 * Google Cloud side, without ripping out that implementation.
 */
export function searchProductImages(query: string): Promise<ImageSearchResult[]> {
  const trimmed = query.trim();
  if (!trimmed) return Promise.resolve([]);

  return imageSearchProvider() === 'serpapi' ? searchSerpApiImages(trimmed) : searchGoogleImages(trimmed);
}

// ── Google Custom Search JSON API ───────────────────────────────────────────

interface GoogleImageSearchItem {
  title?: string;
  link: string;
  image?: { thumbnailLink?: string };
}

interface GoogleImageSearchResponse {
  items?: GoogleImageSearchItem[];
}

const GOOGLE_SEARCH_URL = 'https://www.googleapis.com/customsearch/v1';

/** This project's Programmable Search Engine, scoped to image results —
 * https://cse.google.com/cse?cx=9116fe971734a400e. Not a secret (it's
 * embeddable in a public widget URL), unlike the API key below. */
const GOOGLE_CX = '9116fe971734a400e';

async function searchGoogleImages(query: string): Promise<ImageSearchResult[]> {
  const apiKey = process.env.EXPO_PUBLIC_GOOGLE_CSE_API_KEY;
  if (!apiKey) throw new Error('EXPO_PUBLIC_GOOGLE_CSE_API_KEY no está configurada.');

  const params = new URLSearchParams({
    key: apiKey,
    cx: GOOGLE_CX,
    q: query,
    searchType: 'image',
    num: '10',
    safe: 'active',
    imgDominantColor: 'white',
  });

  const response = await fetch(`${GOOGLE_SEARCH_URL}?${params.toString()}`);

  if (!response.ok) {
    const text = await response.text().catch(() => response.status.toString());
    console.error(`[image-search] (google) GET customsearch/v1 -> ${response.status}`, text);
    throw new Error(`Google Custom Search ${response.status}: ${text}`);
  }

  const data: GoogleImageSearchResponse = await response.json();
  const items = data.items ?? [];
  console.log(`[image-search] (google) "${query}" -> ${items.length} resultado(s)`);

  return items.map(item => ({
    thumbnailUrl: item.image?.thumbnailLink ?? item.link,
    fullUrl: item.link,
    title: item.title ?? '',
  }));
}

// ── SerpApi (Google Images) ─────────────────────────────────────────────────

interface SerpApiImageItem {
  title?: string;
  original: string;
  thumbnail?: string;
}

interface SerpApiResponse {
  images_results?: SerpApiImageItem[];
  error?: string;
}

const SERPAPI_URL = 'https://serpapi.com/search.json';

async function searchSerpApiImages(query: string): Promise<ImageSearchResult[]> {
  const apiKey = process.env.EXPO_PUBLIC_SERPAPI_API_KEY;
  if (!apiKey) throw new Error('EXPO_PUBLIC_SERPAPI_API_KEY no está configurada.');

  const params = new URLSearchParams({
    engine: 'google_images',
    q: query,
    num: '10',
    safe: 'active',
    // Google Images' advanced "color" filter, same intent as the Google CSE
    // path's `imgDominantColor=white` — SerpApi just passes through Google's
    // own `tbs` query-string filters instead of exposing a dedicated param.
    tbs: 'ic:specific,isc:white',
    api_key: apiKey,
  });

  const response = await fetch(`${SERPAPI_URL}?${params.toString()}`);
  const data: SerpApiResponse = await response.json();

  if (!response.ok || data.error) {
    const message = data.error ?? response.status.toString();
    console.error(`[image-search] (serpapi) GET search.json -> ${response.status}`, message);
    throw new Error(`SerpApi: ${message}`);
  }

  const items = data.images_results ?? [];
  console.log(`[image-search] (serpapi) "${query}" -> ${items.length} resultado(s)`);

  // SerpApi's `num` isn't a hard cap for the images engine the way Google's
  // own API treats it — slice to keep the same "first 10" contract either way.
  return items.slice(0, 10).map(item => ({
    thumbnailUrl: item.thumbnail ?? item.original,
    fullUrl: item.original,
    title: item.title ?? '',
  }));
}

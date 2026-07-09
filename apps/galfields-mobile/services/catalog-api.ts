import { apiBaseUrl } from './api-base-url';
import { parseApiErrorMessage } from './api-error';

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const method = init?.method ?? 'GET';
  const url = `${apiBaseUrl()}${path}`;

  const response = await fetch(url, {
    ...init,
    headers: { 'Content-Type': 'application/json', ...init?.headers },
  });

  if (!response.ok) {
    const text = await response.text().catch(() => '');
    console.error(`[catalog-api] ${method} ${path} -> ${response.status}`, text);
    throw new Error(parseApiErrorMessage(response.status, text));
  }

  if (response.status === 204) return undefined as T;
  const json = await response.json();
  console.log(`[catalog-api] ${method} ${path} -> ${response.status}`);
  return json;
}

// ── Categories ──
interface RemoteCategory {
  categoryId: number;
  name: string;
  description: string | null;
}

export interface CatalogCategory {
  id: number;
  name: string;
  description: string;
}

export interface CategoryFormData {
  name: string;
  description: string;
}

const mapCategory = (r: RemoteCategory): CatalogCategory => ({
  id: r.categoryId,
  name: r.name,
  description: r.description ?? '',
});

export const categoriesApi = {
  list: async (): Promise<CatalogCategory[]> =>
    (await request<RemoteCategory[]>('/api/categories')).map(mapCategory),
  create: async (data: CategoryFormData): Promise<CatalogCategory> =>
    mapCategory(await request<RemoteCategory>('/api/categories', { method: 'POST', body: JSON.stringify(data) })),
  update: async (id: number, data: CategoryFormData): Promise<CatalogCategory> =>
    mapCategory(await request<RemoteCategory>(`/api/categories/${id}`, { method: 'PUT', body: JSON.stringify(data) })),
  remove: (id: number): Promise<void> => request<void>(`/api/categories/${id}`, { method: 'DELETE' }),
};

// ── Brands ──
interface RemoteBrand {
  brandId: number;
  name: string;
}

export interface CatalogBrand {
  id: number;
  name: string;
}

export interface BrandFormData {
  name: string;
}

const mapBrand = (r: RemoteBrand): CatalogBrand => ({ id: r.brandId, name: r.name });

export const brandsApi = {
  list: async (): Promise<CatalogBrand[]> => (await request<RemoteBrand[]>('/api/brands')).map(mapBrand),
  create: async (data: BrandFormData): Promise<CatalogBrand> =>
    mapBrand(await request<RemoteBrand>('/api/brands', { method: 'POST', body: JSON.stringify(data) })),
  update: async (id: number, data: BrandFormData): Promise<CatalogBrand> =>
    mapBrand(await request<RemoteBrand>(`/api/brands/${id}`, { method: 'PUT', body: JSON.stringify(data) })),
  remove: (id: number): Promise<void> => request<void>(`/api/brands/${id}`, { method: 'DELETE' }),
};

// ── Locations ──
interface RemoteLocation {
  locationId: number;
  name: string;
  address: string | null;
  phone: string | null;
}

export interface CatalogLocation {
  id: number;
  name: string;
  address: string;
  phone: string;
}

export interface LocationFormData {
  name: string;
  address: string;
  phone: string;
}

const mapLocation = (r: RemoteLocation): CatalogLocation => ({
  id: r.locationId,
  name: r.name,
  address: r.address ?? '',
  phone: r.phone ?? '',
});

export const locationsApi = {
  list: async (): Promise<CatalogLocation[]> => (await request<RemoteLocation[]>('/api/locations')).map(mapLocation),
  create: async (data: LocationFormData): Promise<CatalogLocation> =>
    mapLocation(await request<RemoteLocation>('/api/locations', { method: 'POST', body: JSON.stringify(data) })),
  update: async (id: number, data: LocationFormData): Promise<CatalogLocation> =>
    mapLocation(await request<RemoteLocation>(`/api/locations/${id}`, { method: 'PUT', body: JSON.stringify(data) })),
  remove: (id: number): Promise<void> => request<void>(`/api/locations/${id}`, { method: 'DELETE' }),
};

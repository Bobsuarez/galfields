import { apiBaseUrl } from './api-base-url';
import { parseApiErrorMessage } from './api-error';
import { authenticatedFetch } from './authenticated-fetch';
import { fetchProduct, type RemoteProductUnit } from './products-api';

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const method = init?.method ?? 'GET';
  const url = `${apiBaseUrl()}${path}`;

  const response = await authenticatedFetch(url, {
    ...init,
    headers: { 'Content-Type': 'application/json', ...init?.headers },
  });

  if (!response.ok) {
    const text = await response.text().catch(() => '');
    console.error(`[product-units-api] ${method} ${path} -> ${response.status}`, text);
    throw new Error(parseApiErrorMessage(response.status, text));
  }

  if (response.status === 204) return undefined as T;
  const json = await response.json();
  console.log(`[product-units-api] ${method} ${path} -> ${response.status}`);
  return json;
}

export interface ProductUnit {
  id: number;
  unitName: string;
  conversionFactor: number;
  unitPrice: number;
  barcode: string;
  isBase: boolean;
  active: boolean;
}

export interface ProductUnitFormData {
  unitName: string;
  conversionFactor: number;
  unitPrice: number;
  barcode: string;
}

const mapUnit = (r: RemoteProductUnit): ProductUnit => ({
  id: r.productUnitId,
  unitName: r.unitName,
  conversionFactor: r.conversionFactor,
  unitPrice: r.unitPrice,
  barcode: r.barcode ?? '',
  isBase: r.isBase,
  active: r.active,
});

/**
 * Sale units aren't a standalone list endpoint (see backend/pos's CLAUDE.md,
 * "CRUD (/api/product-variants/{variantId}/units)") - they're only ever
 * read back nested under a variant inside GET /api/products/{productId}
 * (ProductVariantResponse.units). `list` fetches the parent product and
 * picks out this one variant's units; create/update/deactivate hit the
 * real CRUD endpoints directly, scoped to variantId.
 */
export function productUnitsApi(productId: string, variantId: number) {
  return {
    list: async (): Promise<ProductUnit[]> => {
      const product = await fetchProduct(productId);
      const variant = product.variants.find(v => v.variantId === variantId);
      return (variant?.units ?? []).map(mapUnit);
    },
    create: async (data: ProductUnitFormData): Promise<ProductUnit> =>
      mapUnit(
        await request<RemoteProductUnit>(`/api/product-variants/${variantId}/units`, {
          method: 'POST',
          body: JSON.stringify({ ...data, barcode: data.barcode || null }),
        }),
      ),
    update: async (id: number, data: ProductUnitFormData): Promise<ProductUnit> =>
      mapUnit(
        await request<RemoteProductUnit>(`/api/product-variants/${variantId}/units/${id}`, {
          method: 'PUT',
          body: JSON.stringify({ ...data, barcode: data.barcode || null }),
        }),
      ),
    // Soft-deactivate (204 No Content) - never a real delete, matches the
    // backend's contract (see CLAUDE.md above): a unit marked isBase can't
    // be deactivated, the backend 409s if attempted.
    remove: (id: number): Promise<void> =>
      request<void>(`/api/product-variants/${variantId}/units/${id}`, { method: 'DELETE' }),
  };
}

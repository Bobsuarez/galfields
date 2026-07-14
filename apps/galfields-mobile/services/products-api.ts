import { apiBaseUrl } from './api-base-url';
import { parseApiErrorMessage } from './api-error';
import { appendImagePart, jsonPart } from '@/utils/multipart-form';
import type { ProductInput, ProductVariantDraft } from '@/types/product';

export interface RemoteVariantAttribute {
  name: string;
  value: string;
}

export interface RemoteVariant {
  variantId: number;
  sku: string;
  barcode: string;
  price: number;
  costPrice: number;
  stock: number;
  imageUrl: string | null;
  active: boolean;
  attributes: RemoteVariantAttribute[];
}

export interface RemoteProduct {
  productId: number;
  name: string;
  description: string | null;
  categoryId: number | null;
  categoryName: string | null;
  brandId: number | null;
  brandName: string | null;
  imageUrl: string | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
  variants: RemoteVariant[];
}

export interface ProductsPage {
  content: RemoteProduct[];
  number: number;
  totalPages: number;
  totalElements: number;
}

/** Spring's PagedModel nests pagination metadata under a "page" object
 * instead of putting it at the top level — see ProductController#list. */
interface RemoteProductsPage {
  content: RemoteProduct[];
  page: {
    size: number;
    number: number;
    totalElements: number;
    totalPages: number;
  };
}

/** Prefills the edit form from a fetched product — `originalSku` pins each
 * existing variant to its real backend sku (see ProductVariantDraft). */
export function toVariantDraft(variant: RemoteVariant): ProductVariantDraft {
  return {
    barcode: variant.barcode,
    price: String(variant.price),
    costPrice: String(variant.costPrice),
    initialStock: String(variant.stock),
    attributes: variant.attributes,
    imageUri: variant.imageUrl ?? undefined,
    originalSku: variant.sku,
  };
}

/** GET /api/products is paginated (default size 20) — sorted by name so the
 * list reads predictably; see ProductController's SORTABLE_PROPERTIES for
 * the other keys it accepts. */
export async function fetchProducts(page = 0, size = 20): Promise<ProductsPage> {
  const params = new URLSearchParams({ page: String(page), size: String(size), sort: 'name,asc' });
  const url = `${apiBaseUrl()}/api/products?${params.toString()}`;

  const response = await fetch(url);

  if (!response.ok) {
    const text = await response.text().catch(() => '');
    console.error(`[products-api] GET /api/products -> ${response.status}`, text);
    throw new Error(parseApiErrorMessage(response.status, text));
  }

  const raw: RemoteProductsPage = await response.json();
  const result: ProductsPage = {
    content: raw.content,
    number: raw.page.number,
    totalPages: raw.page.totalPages,
    totalElements: raw.page.totalElements,
  };
  console.log(
    `[products-api] GET /api/products -> ${response.status} (page ${result.number + 1}/${result.totalPages}, ${result.content.length} items)`,
  );
  return result;
}

/** GET /api/products/{id} — used to prefill the edit form. */
export async function fetchProduct(productId: string): Promise<RemoteProduct> {
  const response = await fetch(`${apiBaseUrl()}/api/products/${productId}`);

  if (!response.ok) {
    const text = await response.text().catch(() => '');
    console.error(`[products-api] GET /api/products/${productId} -> ${response.status}`, text);
    throw new Error(parseApiErrorMessage(response.status, text));
  }

  return response.json();
}

/** True for locally-picked images (expo file uris); false for a remote
 * https url coming back from the server unchanged. Used on update so we
 * don't try to re-upload an already-hosted image as a file part — RN's
 * FormData only supports local file uris for file parts. */
function isLocalImageUri(uri: string): boolean {
  return !uri.startsWith('http://') && !uri.startsWith('https://');
}

/**
 * A product and its variants are created/updated in a single multipart call:
 * a "product" JSON part (product-level fields only), an optional "image"
 * part for the product's own photo, a "variants" JSON array, and one file
 * part per variant image named "variantImage_<index>" — <index> is the
 * zero-based position of that variant in the "variants" array (see
 * ProductController). Omitting the "image"/"variantImage_<index>" part
 * leaves that image unchanged on update.
 */
async function buildProductFormData(payload: ProductInput): Promise<FormData> {
  const { imageUri, variants, ...productFields } = payload;

  const formData = new FormData();
  formData.append('product', (await jsonPart('product', productFields)) as unknown as Blob);

  if (imageUri && isLocalImageUri(imageUri)) {
    appendImagePart(formData, 'image', imageUri);
  }

  const variantsJson = variants.map(({ imageUri: _variantImage, ...variantFields }) => variantFields);
  formData.append('variants', (await jsonPart('variants', variantsJson)) as unknown as Blob);

  variants.forEach((variant, index) => {
    if (variant.imageUri && isLocalImageUri(variant.imageUri)) {
      appendImagePart(formData, `variantImage_${index}`, variant.imageUri);
    }
  });

  return formData;
}

export async function createProduct(payload: ProductInput): Promise<RemoteProduct> {
  const formData = await buildProductFormData(payload);

  console.log('[products-api] POST /api/products', {
    product: payload,
    hasMainImage: !!payload.imageUri,
  });

  const response = await fetch(`${apiBaseUrl()}/api/products`, {
    method: 'POST',
    body: formData,
  });

  if (!response.ok) {
    const text = await response.text().catch(() => '');
    console.error(`[products-api] POST /api/products -> ${response.status}`, text);
    throw new Error(parseApiErrorMessage(response.status, text));
  }

  const result: RemoteProduct = await response.json();
  console.log('[products-api] created product', result.productId, `variants: ${result.variants.length}`);
  return result;
}

export async function updateProduct(productId: string, payload: ProductInput): Promise<RemoteProduct> {
  const formData = await buildProductFormData(payload);

  console.log('[products-api] PUT /api/products/' + productId, {
    product: payload,
    hasMainImage: !!payload.imageUri,
  });

  const response = await fetch(`${apiBaseUrl()}/api/products/${productId}`, {
    method: 'PUT',
    body: formData,
  });

  if (!response.ok) {
    const text = await response.text().catch(() => '');
    console.error(`[products-api] PUT /api/products/${productId} -> ${response.status}`, text);
    throw new Error(parseApiErrorMessage(response.status, text));
  }

  const result: RemoteProduct = await response.json();
  console.log('[products-api] updated product', result.productId, `variants: ${result.variants.length}`);
  return result;
}

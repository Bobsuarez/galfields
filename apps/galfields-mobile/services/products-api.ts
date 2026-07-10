import * as FileSystem from 'expo-file-system/legacy';
import { apiBaseUrl } from './api-base-url';
import { parseApiErrorMessage } from './api-error';
import { guessImageMimeType, imageFileName } from '@/utils/image-mime';
import type { ProductInput } from '@/types/product';

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

  const result: ProductsPage = await response.json();
  console.log(
    `[products-api] GET /api/products -> ${response.status} (page ${result.number + 1}/${result.totalPages}, ${result.content.length} items)`,
  );
  return result;
}

/**
 * React Native's `Blob` polyfill isn't reliable for in-memory JSON parts
 * (the multipart body it produces can arrive empty/malformed on-device even
 * though the exact same `Blob`+`FormData` code works fine under a
 * spec-compliant fetch implementation) — write the JSON to a temp file
 * instead and attach it the same proven way images already are, as a
 * `{ uri, name, type }` file part.
 */
async function jsonPart(fieldName: string, value: unknown): Promise<{ uri: string; name: string; type: string }> {
  const path = `${FileSystem.cacheDirectory}${fieldName}_${Date.now()}.json`;
  await FileSystem.writeAsStringAsync(path, JSON.stringify(value));
  return { uri: path, name: `${fieldName}.json`, type: 'application/json' };
}

function appendImagePart(formData: FormData, fieldName: string, uri: string): void {
  const mimeType = guessImageMimeType(uri);
  formData.append(fieldName, {
    uri,
    name: imageFileName(fieldName, mimeType),
    type: mimeType,
  } as unknown as Blob);
}

/**
 * A product and its variants are created in a single multipart call: a
 * "product" JSON part (product-level fields only), an optional "image" part
 * for the product's own photo, a "variants" JSON array, and one file part per
 * variant image named "variantImage_<index>" — <index> is the zero-based
 * position of that variant in the "variants" array (see ProductController).
 */
export async function createProduct(payload: ProductInput): Promise<RemoteProduct> {
  const { imageUri, variants, ...productFields } = payload;

  const formData = new FormData();
  formData.append('product', (await jsonPart('product', productFields)) as unknown as Blob);

  if (imageUri) {
    appendImagePart(formData, 'image', imageUri);
  }

  const variantsJson = variants.map(({ imageUri: _variantImage, ...variantFields }) => variantFields);
  formData.append('variants', (await jsonPart('variants', variantsJson)) as unknown as Blob);

  variants.forEach((variant, index) => {
    if (variant.imageUri) {
      appendImagePart(formData, `variantImage_${index}`, variant.imageUri);
    }
  });

  console.log('[products-api] POST /api/products', {
    product: productFields,
    variants: variantsJson,
    hasMainImage: !!imageUri,
    variantImages: variants.map(v => !!v.imageUri),
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

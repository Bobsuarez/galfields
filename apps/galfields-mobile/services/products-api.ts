export interface RemoteCategory {
  categoryId: number;
  name: string;
}

export interface RemoteProduct {
  productId: number;
  name: string;
  description: string | null;
  categoryId: number | null;
  categoryName: string | null;
  brandId: number | null;
  brandName: string | null;
  variantId: number;
  sku: string;
  barcode: string;
  price: number;
  costPrice: number;
  stock: number;
  imageUrl: string | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateProductPayload {
  name: string;
  categoryId: number;
  price: number;
  barcode: string;
  initialStock: number;
  imageUri?: string;
}

function apiBaseUrl(): string {
  const url = process.env.EXPO_PUBLIC_API_BASE_URL;
  if (!url) throw new Error('EXPO_PUBLIC_API_BASE_URL no está configurada.');
  return url;
}

export async function fetchCategories(): Promise<RemoteCategory[]> {
  const response = await fetch(`${apiBaseUrl()}/api/categories`);
  if (!response.ok) {
    throw new Error(`No se pudieron cargar las categorías (${response.status})`);
  }
  return response.json();
}

function guessImageMimeType(uri: string): string {
  const extension = uri.split('.').pop()?.toLowerCase();
  if (extension === 'png') return 'image/png';
  if (extension === 'webp') return 'image/webp';
  return 'image/jpeg';
}

/**
 * The mobile form only collects one price and no SKU, so the sale price
 * doubles as costPrice and the barcode doubles as sku — both are already
 * required+unique on the backend and this keeps the form from growing
 * fields the POS doesn't use yet.
 */
export async function createProduct(payload: CreateProductPayload): Promise<RemoteProduct> {
  const { imageUri, barcode, price, ...rest } = payload;

  const productJson = {
    ...rest,
    barcode,
    price,
    sku: barcode,
    costPrice: price,
  };

  const formData = new FormData();
  formData.append(
    'product',
    new Blob([JSON.stringify(productJson)], { type: 'application/json' }) as unknown as Blob,
  );

  if (imageUri) {
    const mimeType = guessImageMimeType(imageUri);
    formData.append('image', {
      uri: imageUri,
      name: `product.${mimeType === 'image/png' ? 'png' : 'jpg'}`,
      type: mimeType,
    } as unknown as Blob);
  }

  const response = await fetch(`${apiBaseUrl()}/api/products`, {
    method: 'POST',
    body: formData,
  });

  if (!response.ok) {
    const text = await response.text().catch(() => response.status.toString());
    throw new Error(`Error ${response.status} al crear el producto: ${text}`);
  }

  return response.json();
}

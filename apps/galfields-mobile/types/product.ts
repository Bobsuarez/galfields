export interface ProductVariantAttribute {
  name: string;
  value: string;
}

/** One variant row as edited in the add-product form — numeric fields stay
 * strings while the user types, converted to numbers only at submit time.
 * No `sku` here: it's derived from the product name + first attribute value
 * + barcode (see utils/sku.ts), never typed by the user. */
export interface ProductVariantDraft {
  barcode: string;
  price: string;
  costPrice: string;
  initialStock: string;
  attributes: ProductVariantAttribute[];
  imageUri?: string;
}

export function createEmptyVariantDraft(): ProductVariantDraft {
  return { barcode: '', price: '', costPrice: '', initialStock: '', attributes: [] };
}

/** Ready-to-send payload for POST /api/products — mirrors ProductRequest +
 * ProductVariantRequest on the backend (see ProductController). */
export interface ProductVariantInput {
  sku: string;
  barcode: string;
  price: number;
  costPrice: number;
  initialStock: number;
  attributes: ProductVariantAttribute[];
  imageUri?: string;
}

export interface ProductInput {
  name: string;
  description: string;
  categoryId: number;
  brandId: number;
  imageUri?: string;
  variants: ProductVariantInput[];
}

/**
 * Local list-display shape — flattens a multi-variant RemoteProduct into a
 * single-row-per-product summary (first variant's price/barcode, total
 * stock across variants) since the list UI shows one row per product, not
 * per variant. See contexts/products-context.tsx's toLocalProduct.
 */
export interface Product {
  id: string;
  name: string;
  category: string;
  price: number;
  stock: number;
  variantCount: number;
  barcode?: string;
  imageUri?: string;
}

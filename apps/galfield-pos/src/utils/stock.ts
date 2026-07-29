import type { Product, StockStatus } from '../types'

// No `min_stock` column on `products` yet, so "low stock" is a single fixed
// threshold rather than a per-product comparison — swap for a real column
// once one exists. Shared so the sidebar banner and Inventory agree on what
// "low stock" means.
export const LOW_STOCK_THRESHOLD = 5

export function deriveStockStatus(stock: number): StockStatus {
  if (stock <= 0) return 'sin-stock'
  if (stock <= LOW_STOCK_THRESHOLD) return 'stock-bajo'
  return 'en-stock'
}

// Deactivation always wins over a stock badge - it's a different, more
// fundamental reason a product can't be sold. Shared by ProductCard.vue and
// UnitPickerModal.vue so both surfaces agree on when a sale unit is sellable.
export function productUnavailableReason(product: Pick<Product, 'isActive' | 'stockQuantity'>): string | null {
  if (!product.isActive) return 'Desactivado'
  if (product.stockQuantity <= 0) return 'Sin stock'
  return null
}

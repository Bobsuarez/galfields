import { ref } from 'vue'
import { invoke } from '@tauri-apps/api/core'

export type CatalogSyncStep = 'products' | 'categories' | 'payment-methods'

export const CATALOG_SYNC_STEP_LABELS: Record<CatalogSyncStep, string> = {
  products: 'Descargando catálogo de productos...',
  categories: 'Sincronizando categorías...',
  'payment-methods': 'Sincronizando métodos de pago...',
}

interface ProductSyncResult {
  productsFetched: number
  variantsSynced: number
  productsDeactivated: number
}

interface PaymentMethodSyncResult {
  synced: number
  deactivated: number
}

export interface CatalogSyncSummary {
  productsFetched: number
  variantsSynced: number
  productsDeactivated: number
  categoriesSynced: number
  paymentMethodsSynced: number
  paymentMethodsDeactivated: number
}

/**
 * Orchestrates the full "Sincronizar catálogo" button as one sequential
 * chain — products, then categories, then payment methods (each a separate
 * Rust command: sync.rs / catalog_sync.rs) — instead of three buttons.
 * Chained rather than run in parallel so `currentStep` always reflects
 * what's actually in flight, and a failure partway through stops the chain
 * (skipping straight to categories/payment-methods with a stale catalog
 * would be worse than just reporting where it broke).
 */
export function useCatalogSync() {
  const syncing = ref(false)
  const currentStep = ref<CatalogSyncStep | null>(null)
  const error = ref<string | null>(null)
  const summary = ref<CatalogSyncSummary | null>(null)

  async function runSync() {
    syncing.value = true
    error.value = null
    summary.value = null

    try {
      currentStep.value = 'products'
      const products = await invoke<ProductSyncResult>('sync_products')

      currentStep.value = 'categories'
      const categoriesSynced = await invoke<number>('sync_categories')

      currentStep.value = 'payment-methods'
      const paymentMethods = await invoke<PaymentMethodSyncResult>('sync_payment_methods')

      summary.value = {
        productsFetched: products.productsFetched,
        variantsSynced: products.variantsSynced,
        productsDeactivated: products.productsDeactivated,
        categoriesSynced,
        paymentMethodsSynced: paymentMethods.synced,
        paymentMethodsDeactivated: paymentMethods.deactivated,
      }
    } catch (err) {
      const message = err instanceof Error ? err.message : String(err)
      // Products already succeeded if we got past the first step - say so,
      // instead of a bare error that reads like nothing happened at all.
      error.value = currentStep.value === 'products'
        ? message
        : `Productos sincronizados. Falló "${CATALOG_SYNC_STEP_LABELS[currentStep.value!]}": ${message}`
    } finally {
      syncing.value = false
      currentStep.value = null
    }
  }

  return { syncing, currentStep, error, summary, runSync }
}

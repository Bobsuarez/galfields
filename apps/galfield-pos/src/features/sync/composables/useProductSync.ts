import { ref } from 'vue'
import { invoke } from '@tauri-apps/api/core'

export interface SyncSummary {
  productsFetched: number
  variantsSynced: number
  productsDeactivated: number
}

/**
 * Wraps the `sync_products` Tauri command: pulls the full cloud catalog and
 * upserts it into the local `products` table by barcode. User-triggered
 * only (a button in SyncView) - never called automatically.
 */
export function useProductSync() {
  const syncing = ref(false)
  const error = ref<string | null>(null)
  const lastSummary = ref<SyncSummary | null>(null)

  async function runSync() {
    syncing.value = true
    error.value = null
    try {
      lastSummary.value = await invoke<SyncSummary>('sync_products')
    } catch (err) {
      error.value = err instanceof Error ? err.message : String(err)
    } finally {
      syncing.value = false
    }
  }

  return { syncing, error, lastSummary, runSync }
}

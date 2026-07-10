/**
 * Local products, synced from the cloud catalog into SQLite by the
 * Sincronización screen (`sync_products`). Shared low-level data fetch used
 * by both the POS catalog (active-only, grouped by category) and Inventory
 * (full list, searchable) — each builds its own filtering on top of this.
 */

import { ref } from 'vue'
import { invoke } from '@tauri-apps/api/core'
import type { Product } from '../types'

export function useProducts() {
  const products = ref<Product[]>([])
  const isLoading = ref(false)
  const error = ref<string | null>(null)

  async function loadProducts(): Promise<void> {
    isLoading.value = true
    error.value = null
    try {
      products.value = await invoke<Product[]>('get_products')
    } catch (e) {
      error.value = e instanceof Error ? e.message : String(e)
      console.error('[products] load failed:', e)
    } finally {
      isLoading.value = false
    }
  }

  return { products, isLoading, error, loadProducts }
}

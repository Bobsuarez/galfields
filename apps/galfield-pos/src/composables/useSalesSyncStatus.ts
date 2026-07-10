import { ref, onMounted, onUnmounted } from 'vue'
import { listen, type UnlistenFn } from '@tauri-apps/api/event'

interface SalesSyncStatusPayload {
  pendingCount: number
  pushedCount: number
  error: string | null
}

/**
 * Listens for `sales-sync-status` events emitted by `sales_sync.rs` after
 * every push attempt (right after checkout, or the scheduled background
 * retry) - drives the "N ventas pendientes" indicator in AppStatusbar.vue.
 * Purely reactive to what Rust reports; never invokes anything itself (see
 * "Peripheral event model" in CLAUDE.md for the same fire-and-listen shape
 * this follows).
 */
export function useSalesSyncStatus() {
  const pendingCount = ref(0)
  const lastError = ref<string | null>(null)

  let unlisten: UnlistenFn | null = null

  onMounted(async () => {
    unlisten = await listen<SalesSyncStatusPayload>('sales-sync-status', event => {
      pendingCount.value = event.payload.pendingCount
      lastError.value = event.payload.error
    })
  })

  onUnmounted(() => {
    unlisten?.()
  })

  return { pendingCount, lastError }
}

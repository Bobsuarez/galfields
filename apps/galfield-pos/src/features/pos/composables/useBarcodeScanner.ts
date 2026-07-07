import { ref, watch, onUnmounted } from 'vue'
import { type UnlistenFn } from '@tauri-apps/api/event'
import { useBarcodeBus } from '../../../composables/peripherals/useBarcodeBus'
import { useAppConfig } from '../../../composables/useAppConfig'
import { useToast } from '../../../composables/useToast'
import type { Product } from '../../../types'

/**
 * Manages the barcode scanner lifecycle for the POS view.
 *
 * Usage:
 *   const { isConnected } = useBarcodeScanner(product => addProduct(product))
 *
 * Starts the Rust listener on the port configured in `peripherals.barcodePort`
 * and stops it when the component unmounts. Emits a toast when a scanned
 * code has no matching product in the DB.
 *
 * Watches the config value instead of reading it once on mount: `App.vue`'s
 * `loadConfig()` is async (a Tauri round-trip), so on a cold start POSView
 * can mount and read `config.peripherals.barcodePort` before that call
 * resolves — a one-shot `onMounted` read would see the still-empty default,
 * decide there's nothing to start, and never try again, leaving the reader
 * silently never-started (the first scans do nothing until something else
 * happens to remount this composable). Watching with `immediate: true`
 * reacts correctly whether the port is already loaded or arrives moments
 * later, and also restarts the listener for free if the port changes.
 */
export function useBarcodeScanner(onProductFound: (product: Product) => void) {
  const { config }                                              = useAppConfig()
  const { show }                                                = useToast()
  const { startDevice, stopDevice, onBarcodeFound,
          onBarcodeNotFound, onBarcodeStatus, onBarcodeError } = useBarcodeBus()

  const isConnected     = ref(false)
  const activePort      = ref('')
  const unlisteners: UnlistenFn[] = []

  async function start(port: string): Promise<void> {
    activePort.value = port

    // Subscribe to events before starting the listener so no event is missed.
    unlisteners.push(
      await onBarcodeFound(product => {
        onProductFound(product)
      }),

      await onBarcodeNotFound(barcode => {
        show(`Código "${barcode}" no encontrado en inventario`, 'error')
      }),

      await onBarcodeStatus(({ connected, port: p }) => {
        isConnected.value = connected
        if (!connected) show(`Lector desconectado (${p})`, 'error')
      }),

      await onBarcodeError(message => {
        isConnected.value = false
        console.error('[barcode]', message)
        show(`Error en lector: ${message}`, 'error')
      }),
    )

    try {
      await startDevice('barcode', port)
    } catch (e) {
      show(`No se pudo iniciar el lector de código (${port})`, 'error')
      console.error('[barcode] start failed:', e)
    }
  }

  async function stop(): Promise<void> {
    if (activePort.value) {
      try { await stopDevice('barcode') } catch { /* best-effort */ }
      isConnected.value = false
      activePort.value  = ''
    }
    unlisteners.forEach(fn => fn())
    unlisteners.length = 0
  }

  watch(
    () => config.peripherals?.barcodePort,
    async port => {
      if (activePort.value) await stop()
      if (port) await start(port)
    },
    { immediate: true },
  )

  onUnmounted(stop)

  return { isConnected, activePort }
}

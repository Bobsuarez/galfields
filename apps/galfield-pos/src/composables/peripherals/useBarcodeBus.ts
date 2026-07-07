/**
 * Barcode scanner peripheral — continuous listener. Started/stopped via
 * `useListenerDevice`; this composable owns only the barcode-specific event
 * subscriptions (`peripheral-barcode-*`).
 *
 * See "Peripheral event model" in CLAUDE.md.
 */

import { listen, type UnlistenFn } from '@tauri-apps/api/event'
import type { Product } from '../../types'
import { useListenerDevice, type DeviceStatusPayload } from './useListenerDevice'

export type { DeviceStatusPayload }

// Rust emits ScannedProduct which matches the Product interface
export type ScannedProduct = Product

export interface BarcodeNotFoundPayload {
  barcode: string
}

export function useBarcodeBus() {
  const { startDevice, stopDevice } = useListenerDevice()

  function onBarcodeFound(handler: (product: ScannedProduct) => void): Promise<UnlistenFn> {
    return listen<ScannedProduct>('peripheral-barcode-found', e => handler(e.payload))
  }

  function onBarcodeNotFound(handler: (barcode: string) => void): Promise<UnlistenFn> {
    return listen<BarcodeNotFoundPayload>(
      'peripheral-barcode-not-found',
      e => handler(e.payload.barcode),
    )
  }

  function onBarcodeStatus(handler: (payload: DeviceStatusPayload) => void): Promise<UnlistenFn> {
    return listen<DeviceStatusPayload>('peripheral-barcode-status', e => handler(e.payload))
  }

  function onBarcodeError(handler: (message: string) => void): Promise<UnlistenFn> {
    return listen<string>('peripheral-barcode-error', e => handler(e.payload))
  }

  return {
    startDevice,
    stopDevice,
    onBarcodeFound,
    onBarcodeNotFound,
    onBarcodeStatus,
    onBarcodeError,
  }
}

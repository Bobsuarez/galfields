/**
 * Printer peripheral — one-shot action, no continuous listener and no
 * `useListenerDevice` involved. Dispatch with `triggerPrint`, then subscribe
 * to `onPrinterStatus`/`onPrinterError` for the outcome.
 *
 * See "Peripheral event model" in CLAUDE.md.
 */

import { invoke } from '@tauri-apps/api/core'
import { listen, type UnlistenFn } from '@tauri-apps/api/event'
import type { DeviceStatusPayload } from './useListenerDevice'

export interface PrinterInvoiceItem {
  name: string
  quantity: number
  lineTotal: number
}

/** Mirrors Rust's `InvoicePrintPayload` — passed as-is to `trigger_print_invoice`. */
export interface PrinterInvoicePayload {
  storeName: string
  storeTaxId: string
  storeAddress: string
  storePhone: string
  paperWidth: string
  invoiceNumber: string
  date: string
  seller: string
  customer: string
  items: PrinterInvoiceItem[]
  subtotal: number
  discount: number
  total: number
  paymentMethod: string
  amountReceived: number
  changeDue: number
}

export function usePrinterBus() {
  /** Fire-and-forget — report comes via `onPrinterStatus`/`onPrinterError`, not this call. */
  async function triggerPrint(payload: PrinterInvoicePayload, printerPort: string): Promise<void> {
    await invoke('trigger_print_invoice', { payload, printerPort })
  }

  function onPrinterStatus(handler: (payload: DeviceStatusPayload) => void): Promise<UnlistenFn> {
    return listen<DeviceStatusPayload>('peripheral-printer-status', e => handler(e.payload))
  }

  function onPrinterError(handler: (message: string) => void): Promise<UnlistenFn> {
    return listen<string>('peripheral-printer-error', e => handler(e.payload))
  }

  return { triggerPrint, onPrinterStatus, onPrinterError }
}

/**
 * Cash drawer peripheral — one-shot action (pulses the kick-out line wired
 * through the printer's RJ11 port), no continuous listener. Dispatch with
 * `triggerCashDrawer`, then subscribe to `onCashDrawerStatus`/`onCashDrawerError`
 * for the outcome.
 *
 * See "Peripheral event model" in CLAUDE.md.
 */

import { invoke } from '@tauri-apps/api/core'
import { listen, type UnlistenFn } from '@tauri-apps/api/event'
import type { DeviceStatusPayload } from './useListenerDevice'

export function useCashDrawerBus() {
  /** Fire-and-forget — report comes via `onCashDrawerStatus`/`onCashDrawerError`, not this call. */
  async function triggerCashDrawer(printerPort: string): Promise<void> {
    await invoke('trigger_open_cash_drawer', { printerPort })
  }

  function onCashDrawerStatus(handler: (payload: DeviceStatusPayload) => void): Promise<UnlistenFn> {
    return listen<DeviceStatusPayload>('peripheral-cash-drawer-status', e => handler(e.payload))
  }

  function onCashDrawerError(handler: (message: string) => void): Promise<UnlistenFn> {
    return listen<string>('peripheral-cash-drawer-error', e => handler(e.payload))
  }

  return { triggerCashDrawer, onCashDrawerStatus, onCashDrawerError }
}

/**
 * Generic continuous-listener peripheral control — thin wrapper around the
 * Rust `start_peripheral_listener` / `stop_peripheral_listener` commands.
 * Knows nothing about any specific device; device-specific composables
 * (e.g. `useBarcodeBus.ts`) use this for their start/stop lifecycle and add
 * their own event subscriptions on top.
 *
 * See "Peripheral event model" in CLAUDE.md.
 */

import { invoke } from '@tauri-apps/api/core'

/** Generic `{ connected, port }` shape emitted as `peripheral-<device>-status`. */
export interface DeviceStatusPayload {
  connected: boolean
  port: string
}

export function useListenerDevice() {
  async function startDevice(deviceType: string, port: string): Promise<void> {
    await invoke('start_peripheral_listener', { deviceType, port })
  }

  async function stopDevice(deviceType: string): Promise<void> {
    await invoke('stop_peripheral_listener', { deviceType })
  }

  return { startDevice, stopDevice }
}

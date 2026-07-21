/**
 * System-level serial port permissions — one-shot action, not tied to a
 * specific device (it's an OS-level grant every serial peripheral needs).
 * Dispatch with `triggerApplyPermissions`, then subscribe to
 * `onPermissionsStatus`/`onPermissionsError` for the outcome.
 *
 * See "Peripheral event model" in CLAUDE.md.
 */

import { invoke } from '@tauri-apps/api/core'
import { listen, type UnlistenFn } from '@tauri-apps/api/event'

export interface PermissionsResultPayload {
  alreadyGranted: boolean
  message: string
}

export function useSystemPermissionsBus() {
  /** Fire-and-forget — report comes via `onPermissionsStatus`/`onPermissionsError`, not this call. */
  async function triggerApplyPermissions(): Promise<void> {
    await invoke('trigger_apply_port_permissions')
  }

  function onPermissionsStatus(handler: (payload: PermissionsResultPayload) => void): Promise<UnlistenFn> {
    return listen<PermissionsResultPayload>('peripheral-permissions-status', e => handler(e.payload))
  }

  function onPermissionsError(handler: (message: string) => void): Promise<UnlistenFn> {
    return listen<string>('peripheral-permissions-error', e => handler(e.payload))
  }

  return { triggerApplyPermissions, onPermissionsStatus, onPermissionsError }
}

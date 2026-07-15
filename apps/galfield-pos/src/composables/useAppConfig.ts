import { reactive, watchEffect } from 'vue'
import { invoke } from '@tauri-apps/api/core'
import type { ConfigSettings } from '../types'
import { DEFAULT_SETTINGS, applyRecord } from '../utils/settingsMapper'

// ── Module-level singleton ──────────────────────────────────────────────────
// Shared across the entire app without Pinia.
// Any component that reads from `config` will react to changes automatically.

const config = reactive<ConfigSettings>(JSON.parse(JSON.stringify(DEFAULT_SETTINGS)))
let loaded = false

// ── CSS variable helpers ────────────────────────────────────────────────────

function darkenHex(hex: string, percent: number): string {
  const n      = parseInt(hex.replace('#', ''), 16)
  const factor = 1 - percent / 100
  const r      = Math.max(0, Math.round(((n >> 16) & 0xff) * factor))
  const g      = Math.max(0, Math.round(((n >>  8) & 0xff) * factor))
  const b      = Math.max(0, Math.round(( n        & 0xff) * factor))
  return `#${r.toString(16).padStart(2,'0')}${g.toString(16).padStart(2,'0')}${b.toString(16).padStart(2,'0')}`
}

function hexToRgba(hex: string, alpha: number): string {
  const n = parseInt(hex.replace('#', ''), 16)
  return `rgba(${(n >> 16) & 0xff}, ${(n >> 8) & 0xff}, ${n & 0xff}, ${alpha})`
}

/** Shifts each RGB channel by `amount` (positive = lighten, negative = darken). */
function shiftHex(hex: string, amount: number): string {
  const n     = parseInt(hex.replace('#', ''), 16)
  const clamp = (v: number) => Math.max(0, Math.min(255, v))
  const r     = clamp(((n >> 16) & 0xff) + amount)
  const g     = clamp(((n >>  8) & 0xff) + amount)
  const b     = clamp(( n        & 0xff) + amount)
  return `#${r.toString(16).padStart(2,'0')}${g.toString(16).padStart(2,'0')}${b.toString(16).padStart(2,'0')}`
}

/** Returns true when the hex color is perceptually light. */
function isLight(hex: string): boolean {
  const n = parseInt(hex.replace('#', ''), 16)
  const l = (0.299 * ((n >> 16) & 0xff) + 0.587 * ((n >> 8) & 0xff) + 0.114 * (n & 0xff)) / 255
  return l > 0.5
}

function applyStyles(styles: ConfigSettings['styles']): void {
  const root = document.documentElement

  // Direction: dark bg → shift positive (lighten surfaces).
  //            light bg → shift negative (darken surfaces).
  const dir = isLight(styles.bgColor) ? -1 : 1

  // ── Backgrounds ───────────────────────────────────────────────────────────
  root.style.setProperty('--color-bg',           styles.bgColor)
  root.style.setProperty('--color-surface',      shiftHex(styles.lightBg,  dir * 5))
  root.style.setProperty('--color-surface-2',    styles.lightBg)
  root.style.setProperty('--color-surface-3',    shiftHex(styles.lightBg,  dir * 8))
  root.style.setProperty('--color-cream',        styles.lightText)   // semantic alias

  // ── Primary & accents ─────────────────────────────────────────────────────
  root.style.setProperty('--color-primary',       styles.primaryColor)
  root.style.setProperty('--color-primary-hover', darkenHex(styles.primaryColor, 12))
  root.style.setProperty('--color-accent',        styles.secondaryText)

  // ── Text ──────────────────────────────────────────────────────────────────
  root.style.setProperty('--color-text',          styles.lightText)
  root.style.setProperty('--color-text-muted',    hexToRgba(styles.lightText, 0.5))
  root.style.setProperty('--color-text-dim',      hexToRgba(styles.lightText, 0.3))

  // ── Borders ───────────────────────────────────────────────────────────────
  root.style.setProperty('--color-border',        hexToRgba(styles.primaryColor, 0.50))
  root.style.setProperty('--color-border-strong', hexToRgba(styles.primaryColor, 0.30))
}

// Runs immediately and every time config.styles changes
watchEffect(() => applyStyles(config.styles))

// ── Composable ──────────────────────────────────────────────────────────────

export function useAppConfig() {
  async function loadConfig(): Promise<void> {
    if (loaded) return
    try {
      const record = await invoke<Record<string, string>>('get_settings')
      applyRecord(config, record)
    } catch (e) {
      console.error('[app-config] Failed to load from DB, using defaults:', e)
    } finally {
      loaded = true
    }
  }

  async function reloadConfig(): Promise<void> {
    loaded = false
    await loadConfig()
  }

  return { config, loadConfig, reloadConfig }
}

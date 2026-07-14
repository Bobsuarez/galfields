import { ref, onMounted, onUnmounted } from 'vue'
import { invoke } from '@tauri-apps/api/core'
import type { StockAlert } from '../types'
import { LOW_STOCK_THRESHOLD } from '../utils/stock'

const LOW_STOCK_LIMIT = 5
const CYCLE_INTERVAL_MS   = 10 * 60 * 1000

const mascotImages = Object.values(
  import.meta.glob('../assets/images/banner/*.png', { eager: true, import: 'default' }),
) as string[]

interface SalesSummary {
  totalSales: number
}

function todayDateStr(): string {
  const d = new Date()
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

function pickRandomMascot(current: string): string {
  if (mascotImages.length === 0) return current
  if (mascotImages.length === 1) return mascotImages[0]
  let next = current
  while (next === current) {
    next = mascotImages[Math.floor(Math.random() * mascotImages.length)]
  }
  return next
}

// ── Module-level singleton ──────────────────────────────────────────────────
// AppSidebar.vue is the only caller and mounts once for the app's lifetime,
// but the state lives at module scope (like useAppConfig.ts) so other
// composables (useCheckout.ts) can force a refresh via `refreshSidebarBanner`
// without needing a component instance/ref to reach into.

const activeView       = ref<'stock' | 'sales'>('stock')
const lowStockItems    = ref<StockAlert[]>([])
const todaySalesTotal  = ref(0)
const bannerTimestamp  = ref(new Date())
const mascotSrc        = ref(mascotImages[0] ?? '')

let intervalId: ReturnType<typeof setInterval> | null = null

async function loadLowStock(): Promise<void> {
  try {
    lowStockItems.value = await invoke<StockAlert[]>('get_low_stock_products', {
      threshold: LOW_STOCK_THRESHOLD,
      limit: LOW_STOCK_LIMIT,
    })
  } catch (e) {
    console.error('[sidebar-banner] load low stock failed:', e)
  }
}

async function loadTodaySales(): Promise<void> {
  try {
    const today = todayDateStr()
    const summary = await invoke<SalesSummary>('get_sales_summary', {
      dateFrom: today,
      dateTo: today,
    })
    todaySalesTotal.value = summary.totalSales
    bannerTimestamp.value = new Date()
  } catch (e) {
    console.error('[sidebar-banner] load today sales failed:', e)
  }
}

async function cycle(): Promise<void> {
  mascotSrc.value = pickRandomMascot(mascotSrc.value)
  if (activeView.value === 'stock') {
    activeView.value = 'sales'
    await loadTodaySales()
  } else {
    activeView.value = 'stock'
    await loadLowStock()
  }
}

/**
 * Forces an immediate re-fetch of both the low-stock list and today's sales
 * total, bypassing the 10-minute auto-cycle. Called right after a sale is
 * created (see useCheckout.ts) so the banner reflects it without waiting for
 * the next cycle — otherwise a completed sale never shows up until the timer
 * happens to land on the "sales" view again.
 */
export async function refreshSidebarBanner(): Promise<void> {
  await Promise.all([loadLowStock(), loadTodaySales()])
}

/**
 * Drives the sidebar's mini banner: alternates every `CYCLE_INTERVAL_MS`
 * between "stock bajo" (real low-stock products) and "venta del día" (today's
 * sales total), fading between states, and swaps the mascot image to a
 * random pick from `src/assets/images/banner/` on each cycle.
 */
export function useSidebarBanner() {
  onMounted(() => {
    loadLowStock()
    intervalId = setInterval(cycle, CYCLE_INTERVAL_MS)
  })

  onUnmounted(() => {
    if (intervalId) clearInterval(intervalId)
  })

  return {
    activeView,
    lowStockItems,
    todaySalesTotal,
    bannerTimestamp,
    mascotSrc,
  }
}

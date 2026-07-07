import { ref, computed, onMounted } from 'vue'
import { invoke } from '@tauri-apps/api/core'
import { useToast } from '../../../composables/useToast'

export interface KpiMetric {
  label: string
  value: number
  change: number
  prefix?: string
}

export interface DailySales {
  label: string
  current: number
  previous: number
}

export interface CategorySales {
  label: string
  value: number
  color: string
}

export interface HourlySales {
  label: string
  value: number
}

export interface TopProduct {
  rank: number
  name: string
  revenue: number
  quantity: number
}

export interface TopCajero {
  name: string
  sales: number
  transactions: number
}

export interface LowStockItem {
  name: string
  currentStock: number
  minStock: number
  sales: number
  status: 'critico' | 'bajo'
}

export interface FinancialData {
  grossSales: number
  discounts: number
  netSales: number
  // Needs a cost column on `products` (none exists yet) — omitted until then,
  // `FinancialSummary.vue` shows a "próximamente" placeholder in their place.
  cogs?: number
  grossMargin?: number
  marginPercent?: number
}

const DAILY_SALES: DailySales[] = [
  { label: 'Lun', current: 3200, previous: 2800 },
  { label: 'Mar', current: 2800, previous: 3100 },
  { label: 'Mié', current: 4500, previous: 3200 },
  { label: 'Jue', current: 3800, previous: 2900 },
  { label: 'Vie', current: 5200, previous: 4100 },
  { label: 'Sáb', current: 4800, previous: 3800 },
  { label: 'Dom', current: 3500, previous: 2600 },
]

const CATEGORY_SALES: CategorySales[] = [
  { label: 'Bebidas', value: 35, color: '#2196F3' },
  { label: 'Comida', value: 28, color: '#4CAF50' },
  { label: 'Snacks', value: 17, color: '#FF9800' },
  { label: 'Lácteos', value: 12, color: '#64B5F6' },
  { label: 'Limpieza', value: 5, color: '#00BCD4' },
  { label: 'Otros', value: 3, color: '#9E9E9E' },
]

const HOURLY_SALES: HourlySales[] = [
  { label: '6am', value: 45000 },
  { label: '9am', value: 125000 },
  { label: '12pm', value: 210000 },
  { label: '3pm', value: 168000 },
  { label: '6pm', value: 290000 },
  { label: '9pm', value: 108000 },
]

const TOP_PRODUCTS_LIMIT = 8

const TOP_CAJEROS: TopCajero[] = [
  { name: 'Ana María', sales: 23900, transactions: 45 },
  { name: 'Luis Torres', sales: 16800, transactions: 32 },
  { name: 'María José', sales: 11400, transactions: 28 },
  { name: 'Carlos Ruiz', sales: 9200, transactions: 21 },
  { name: 'Julia Sánchez', sales: 7800, transactions: 18 },
  { name: 'Pedro Mora', sales: 4900, transactions: 12 },
  { name: 'Rosa Díaz', sales: 3200, transactions: 8 },
]

const LOW_STOCK: LowStockItem[] = [
  { name: 'Aceite Vegetal 1L', currentStock: 2, minStock: 10, sales: 8, status: 'critico' },
  { name: 'Pan de Molde', currentStock: 3, minStock: 10, sales: 8, status: 'critico' },
  { name: 'Papel Higiénico x4', currentStock: 4, minStock: 10, sales: 4, status: 'bajo' },
  { name: 'Leche 2L', currentStock: 8, minStock: 20, sales: 19, status: 'bajo' },
  { name: 'Mantequilla 250g', currentStock: 6, minStock: 10, sales: 8, status: 'bajo' },
]

interface SalesSummary {
  totalSales: number
  saleCount: number
  averageTicket: number
  itemsSold: number
}

// Local DB only keeps the current month's data long-term (older sales will
// eventually live on a remote server, for administrative reasons), so a
// range spanning more than a month needs extra verification. This is a
// placeholder for that future remote-auth flow, not real security — swap
// for a real check once the remote server exists.
const REPORTS_DUMMY_PASSWORD = '0000'
const MONTH_LIMIT_DAYS = 31

function toDateStr(d: Date): string {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

function daysBetween(from: string, to: string): number {
  const ms = new Date(to).getTime() - new Date(from).getTime()
  return Math.round(ms / (1000 * 60 * 60 * 24))
}

/** Same-length period immediately preceding `dateFrom`, for the "vs previous" comparison. */
function previousPeriod(dateFrom: string, dateTo: string): { from: string; to: string } {
  const lengthDays = daysBetween(dateFrom, dateTo) + 1
  const from = new Date(dateFrom)
  const to = new Date(dateFrom)
  to.setDate(to.getDate() - 1)
  from.setDate(from.getDate() - lengthDays)
  return { from: toDateStr(from), to: toDateStr(to) }
}

function percentChange(current: number, previous: number): number {
  if (previous === 0) return 0
  return Math.round(((current - previous) / previous) * 1000) / 10
}

export function useReports() {
  const { show } = useToast()

  const today          = new Date()
  const firstOfMonth   = new Date(today.getFullYear(), today.getMonth(), 1)
  const dateFrom       = ref(toDateStr(firstOfMonth))
  const dateTo         = ref(toDateStr(today))

  const kpis            = ref<KpiMetric[]>([])
  const topProducts     = ref<TopProduct[]>([])
  const financial       = ref<FinancialData>({ grossSales: 0, discounts: 0, netSales: 0 })
  const isLoadingKpis   = ref(false)
  const showPasswordGate = ref(false)
  const isRangeUnlocked  = ref(false)

  const exceedsMonthLimit = computed(() => daysBetween(dateFrom.value, dateTo.value) > MONTH_LIMIT_DAYS)

  async function fetchSummary(from: string, to: string): Promise<SalesSummary> {
    return invoke<SalesSummary>('get_sales_summary', { dateFrom: from, dateTo: to })
  }

  async function fetchTopProducts(from: string, to: string): Promise<TopProduct[]> {
    return invoke<TopProduct[]>('get_top_products', { dateFrom: from, dateTo: to, limit: TOP_PRODUCTS_LIMIT })
  }

  async function fetchFinancialSummary(from: string, to: string): Promise<FinancialData> {
    return invoke<FinancialData>('get_financial_summary', { dateFrom: from, dateTo: to })
  }

  async function loadReportsData(): Promise<void> {
    isLoadingKpis.value = true
    try {
      const previous = previousPeriod(dateFrom.value, dateTo.value)
      const [current, prior, products, financials] = await Promise.all([
        fetchSummary(dateFrom.value, dateTo.value),
        fetchSummary(previous.from, previous.to),
        fetchTopProducts(dateFrom.value, dateTo.value),
        fetchFinancialSummary(dateFrom.value, dateTo.value),
      ])

      kpis.value = [
        {
          label: 'Ventas Totales',
          value: current.totalSales,
          change: percentChange(current.totalSales, prior.totalSales),
          prefix: '$',
        },
        {
          label: 'Número de Ventas',
          value: current.saleCount,
          change: percentChange(current.saleCount, prior.saleCount),
        },
        {
          label: 'Ticket Promedio',
          value: Math.round(current.averageTicket),
          change: percentChange(current.averageTicket, prior.averageTicket),
          prefix: '$',
        },
        {
          label: 'Artículos Vendidos',
          value: current.itemsSold,
          change: percentChange(current.itemsSold, prior.itemsSold),
        },
      ]
      topProducts.value = products
      financial.value    = financials
    } catch (e) {
      console.error('[reports] load data failed:', e)
      show('No se pudieron cargar los indicadores', 'error')
    } finally {
      isLoadingKpis.value = false
    }
  }

  function applyDateRange(): void {
    if (exceedsMonthLimit.value && !isRangeUnlocked.value) {
      showPasswordGate.value = true
      return
    }
    loadReportsData()
  }

  function confirmPasswordGate(password: string): void {
    if (password !== REPORTS_DUMMY_PASSWORD) {
      show('Contraseña incorrecta', 'error')
      return
    }
    isRangeUnlocked.value  = true
    showPasswordGate.value = false
    loadReportsData()
  }

  function cancelPasswordGate(): void {
    showPasswordGate.value = false
  }

  const lineLabels = DAILY_SALES.map(d => d.label)
  const lineSeries = [
    { name: 'Esta semana', data: DAILY_SALES.map(d => d.current), color: '#F28D35' },
    { name: 'Semana anterior', data: DAILY_SALES.map(d => d.previous), color: '#8C501B', dashed: true },
  ]

  const barData = HOURLY_SALES.map(h => ({ label: h.label, value: h.value }))

  onMounted(loadReportsData)

  return {
    dateFrom,
    dateTo,
    kpis,
    isLoadingKpis,
    showPasswordGate,
    applyDateRange,
    confirmPasswordGate,
    cancelPasswordGate,
    lineLabels,
    lineSeries,
    categorySales: CATEGORY_SALES,
    barData,
    topProducts,
    topCajeros: TOP_CAJEROS,
    lowStock: LOW_STOCK,
    financial,
  }
}

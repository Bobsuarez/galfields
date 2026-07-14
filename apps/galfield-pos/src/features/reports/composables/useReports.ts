import { ref, computed, onMounted } from 'vue'
import { invoke } from '@tauri-apps/api/core'
import { useToast } from '../../../composables/useToast'
import { LOW_STOCK_THRESHOLD } from '../../../utils/stock'

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

export interface PaymentMethodSales {
  method: string
  total: number
  saleCount: number
}

export interface ProductNoMovement {
  name: string
  category: string
  stock: number
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

// Cycled through for however many categories/payment methods come back —
// color assignment is a presentation concern, not something the backend
// should own.
const SEGMENT_COLORS = ['#F28D35', '#4CAF50', '#2196F3', '#FF9800', '#64B5F6', '#00BCD4', '#9E9E9E', '#8C501B']

const TOP_PRODUCTS_LIMIT       = 8
const LOW_STOCK_LIMIT          = 10
const NO_MOVEMENT_LIMIT        = 10

interface SalesSummary {
  totalSales: number
  saleCount: number
  averageTicket: number
  itemsSold: number
}

interface DailySalesRow {
  date: string
  total: number
}

interface CategorySalesRow {
  category: string
  total: number
}

interface HourlySalesRow {
  hour: string
  total: number
}

interface LowStockReportRow {
  productName: string
  currentStock: number
  threshold: number
  unitsSold: number
  status: 'critico' | 'bajo'
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

function shortDateLabel(dateStr: string): string {
  const [, m, d] = dateStr.split('-')
  return `${d}/${m}`
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

  const kpis             = ref<KpiMetric[]>([])
  const topProducts      = ref<TopProduct[]>([])
  const financial        = ref<FinancialData>({ grossSales: 0, discounts: 0, netSales: 0 })
  const dailySales       = ref<DailySales[]>([])
  const categorySales    = ref<CategorySales[]>([])
  const hourlySales      = ref<HourlySales[]>([])
  const paymentMethods   = ref<PaymentMethodSales[]>([])
  const productsNoMovement = ref<ProductNoMovement[]>([])
  const lowStock          = ref<LowStockItem[]>([])
  const isLoadingKpis     = ref(false)
  const showPasswordGate  = ref(false)
  const isRangeUnlocked   = ref(false)

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

  async function fetchDailySales(from: string, to: string): Promise<DailySalesRow[]> {
    return invoke<DailySalesRow[]>('get_sales_by_day', { dateFrom: from, dateTo: to })
  }

  async function fetchCategorySales(from: string, to: string): Promise<CategorySalesRow[]> {
    return invoke<CategorySalesRow[]>('get_sales_by_category', { dateFrom: from, dateTo: to })
  }

  async function fetchHourlySales(from: string, to: string): Promise<HourlySalesRow[]> {
    return invoke<HourlySalesRow[]>('get_sales_by_hour', { dateFrom: from, dateTo: to })
  }

  async function fetchPaymentMethodSales(from: string, to: string): Promise<PaymentMethodSales[]> {
    return invoke<PaymentMethodSales[]>('get_sales_by_payment_method', { dateFrom: from, dateTo: to })
  }

  async function fetchProductsNoMovement(from: string, to: string): Promise<ProductNoMovement[]> {
    const rows = await invoke<{ productName: string; category: string; stockQuantity: number }[]>(
      'get_products_without_movement',
      { dateFrom: from, dateTo: to, limit: NO_MOVEMENT_LIMIT },
    )
    return rows.map(r => ({ name: r.productName, category: r.category || 'Sin categoría', stock: r.stockQuantity }))
  }

  async function fetchLowStock(from: string, to: string): Promise<LowStockItem[]> {
    const rows = await invoke<LowStockReportRow[]>('get_low_stock_report', {
      dateFrom: from,
      dateTo: to,
      threshold: LOW_STOCK_THRESHOLD,
      limit: LOW_STOCK_LIMIT,
    })
    return rows.map(r => ({
      name: r.productName,
      currentStock: r.currentStock,
      minStock: r.threshold,
      sales: r.unitsSold,
      status: r.status,
    }))
  }

  async function loadReportsData(): Promise<void> {
    isLoadingKpis.value = true
    try {
      const previous = previousPeriod(dateFrom.value, dateTo.value)
      const [
        current, prior, products, financials,
        days, daysPrior, categories, hours, methods, noMovement, low,
      ] = await Promise.all([
        fetchSummary(dateFrom.value, dateTo.value),
        fetchSummary(previous.from, previous.to),
        fetchTopProducts(dateFrom.value, dateTo.value),
        fetchFinancialSummary(dateFrom.value, dateTo.value),
        fetchDailySales(dateFrom.value, dateTo.value),
        fetchDailySales(previous.from, previous.to),
        fetchCategorySales(dateFrom.value, dateTo.value),
        fetchHourlySales(dateFrom.value, dateTo.value),
        fetchPaymentMethodSales(dateFrom.value, dateTo.value),
        fetchProductsNoMovement(dateFrom.value, dateTo.value),
        fetchLowStock(dateFrom.value, dateTo.value),
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

      // Both series are the same length (each spans its own full date
      // range), zipped by day-offset rather than by matching calendar date —
      // "day 1 of this period" vs "day 1 of the previous period", same idea
      // as the KPI comparison above.
      dailySales.value = days.map((d, i) => ({
        label: shortDateLabel(d.date),
        current: d.total,
        previous: daysPrior[i]?.total ?? 0,
      }))

      categorySales.value = categories.map((c, i) => ({
        label: c.category,
        value: c.total,
        color: SEGMENT_COLORS[i % SEGMENT_COLORS.length],
      }))

      hourlySales.value = hours.map(h => ({ label: `${h.hour}h`, value: h.total }))

      paymentMethods.value    = methods
      productsNoMovement.value = noMovement
      lowStock.value           = low
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

  const lineLabels = computed(() => dailySales.value.map(d => d.label))
  const lineSeries = computed(() => [
    { name: 'Este periodo', data: dailySales.value.map(d => d.current), color: '#F28D35' },
    { name: 'Periodo anterior', data: dailySales.value.map(d => d.previous), color: '#8C501B', dashed: true },
  ])

  const barData = computed(() => hourlySales.value.map(h => ({ label: h.label, value: h.value })))

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
    categorySales,
    barData,
    topProducts,
    paymentMethods,
    productsNoMovement,
    lowStock,
    financial,
  }
}

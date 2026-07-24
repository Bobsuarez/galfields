import { ref } from 'vue'
import { invoke } from '@tauri-apps/api/core'

export interface SaleListRow {
  saleId: number
  invoiceNumber: string
  createdAt: string
  total: number
  paymentMethodName: string
  cancelledAt: string | null
}

export interface SaleDetailItem {
  productName: string
  quantity: number
  unitPrice: number
  subtotal: number
}

export interface SaleDetail {
  saleId: number
  invoiceNumber: string
  createdAt: string
  subtotal: number
  discount: number
  total: number
  paymentMethodName: string
  cancelledAt: string | null
  items: SaleDetailItem[]
}

/** Browsing and cancelling past sales — see sale_history.rs. No pagination
 * here on purpose (last 200, filterable by invoice number), matching the
 * deliberately small scope of that Rust module. */
export function useInvoiceHistory() {
  const sales      = ref<SaleListRow[]>([])
  const isLoading   = ref(false)
  const searchQuery = ref('')

  const selected      = ref<SaleDetail | null>(null)
  const isLoadingDetail = ref(false)
  const isCancelling  = ref(false)
  const detailError   = ref<string | null>(null)

  async function loadSales() {
    isLoading.value = true
    try {
      sales.value = await invoke<SaleListRow[]>('list_sales', { search: searchQuery.value.trim() || null })
    } catch (e) {
      console.error('[invoice-history] load failed:', e)
    } finally {
      isLoading.value = false
    }
  }

  async function openSale(saleId: number) {
    selected.value = null
    detailError.value = null
    isLoadingDetail.value = true
    try {
      selected.value = await invoke<SaleDetail>('get_sale_detail', { saleId })
    } catch (e) {
      detailError.value = String(e)
    } finally {
      isLoadingDetail.value = false
    }
  }

  function closeSale() {
    selected.value = null
    detailError.value = null
  }

  async function cancelSale(saleId: number): Promise<boolean> {
    isCancelling.value = true
    try {
      await invoke('cancel_sale', { saleId })
      await Promise.all([loadSales(), openSale(saleId)])
      return true
    } catch (e) {
      detailError.value = String(e)
      return false
    } finally {
      isCancelling.value = false
    }
  }

  return {
    sales,
    isLoading,
    searchQuery,
    selected,
    isLoadingDetail,
    isCancelling,
    detailError,
    loadSales,
    openSale,
    closeSale,
    cancelSale,
  }
}

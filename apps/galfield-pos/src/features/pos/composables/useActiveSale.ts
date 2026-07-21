import { ref, computed, onMounted } from 'vue'
import { invoke } from '@tauri-apps/api/core'
import { useAppConfig } from '../../../composables/useAppConfig'
import { usePaymentMethods } from '../../../composables/usePaymentMethods'
import { useToast } from '../../../composables/useToast'
import type { CartItem, PendingSale, Product } from '../../../types'

export const SALE_ICONS = [
  { key: 'user',     emoji: '👤', label: 'Cliente'   },
  { key: 'table',    emoji: '🍽️', label: 'Mesa'      },
  { key: 'whatsapp', emoji: '💬', label: 'WhatsApp'  },
  { key: 'phone',    emoji: '📱', label: 'Teléfono'  },
  { key: 'delivery', emoji: '🛵', label: 'Delivery'  },
  { key: 'online',   emoji: '🌐', label: 'Online'    },
]

export function getSaleEmoji(iconKey: string): string {
  return SALE_ICONS.find(i => i.key === iconKey)?.emoji ?? '👤'
}

export function useActiveSale() {
  const { config } = useAppConfig()
  const { paymentMethods, loadPaymentMethods } = usePaymentMethods()
  const { show: showToast } = useToast()

  const customerName  = ref('')
  const cartItems     = ref<CartItem[]>([])
  const pendingSales  = ref<PendingSale[]>([])
  const showSaveModal = ref(false)

  // Set by `resumeSale` when the active cart came from a parked sale, so
  // `requestSave` can update that same row (same id/label/iconKey) instead
  // of asking for a name again and creating a duplicate. Cleared by
  // `newSale` — a genuinely new sale still asks for a name the first time.
  const activePendingSale = ref<{ id: string; label: string; iconKey: string } | null>(null)

  const subtotal = computed(() =>
    cartItems.value.reduce((sum, item) => sum + item.unitPrice * item.quantity, 0),
  )
  const discount = ref(0)
  const total    = computed(() => subtotal.value - discount.value)

  // Cash tendered by the customer, and the change owed back. 0 means the
  // cashier isn't using the calculator for this sale (treated as exact
  // payment when the sale is created — see Rust's `create_sale`).
  const amountReceived = ref(0)
  const changeDue      = computed(() => amountReceived.value > 0 ? amountReceived.value - total.value : 0)

  // Per-sale tender type, editable via PaymentMethodModal. Falls back to
  // "Efectivo" until the configured default is confirmed against the loaded
  // `payment_method` table (see `loadPaymentMethods` below).
  const paymentMethod = ref(config.defaults.paymentMethod || 'Efectivo')
  const paymentMethodId = computed(
    () => paymentMethods.value.find(m => m.name === paymentMethod.value)?.id ?? null,
  )
  const showPaymentModal = ref(false)

  /**
   * Gates the checkout button: cart can't be empty, the chosen tender type
   * must resolve to a real row in `payment_method` (guards against
   * checking out before the table has loaded), and for cash sales the
   * amount tendered must actually cover the total (no negative change).
   * Other tender types (Nequi, cards, ...) have no "change" concept to check.
   */
  const canCheckout = computed(() => {
    if (cartItems.value.length === 0) return false
    if (paymentMethodId.value === null) return false
    if (paymentMethod.value === 'Efectivo') {
      return amountReceived.value > 0 && changeDue.value >= 0
    }
    return true
  })

  /** The configured default if it's a real row in `payment_method`, else "Efectivo", else whatever loaded first. */
  function resolveDefaultPaymentMethod(): string {
    const configured = config.defaults.paymentMethod
    if (paymentMethods.value.some(m => m.name === configured)) return configured
    return paymentMethods.value.find(m => m.name === 'Efectivo')?.name
      ?? paymentMethods.value[0]?.name
      ?? 'Efectivo'
  }

  async function loadAndDefaultPaymentMethod() {
    await loadPaymentMethods()
    const hasCurrent = paymentMethods.value.some(m => m.name === paymentMethod.value)
    if (!hasCurrent) paymentMethod.value = resolveDefaultPaymentMethod()
  }

  function openPaymentMethodModal() {
    showPaymentModal.value = true
  }

  function selectPaymentMethod(key: string) {
    paymentMethod.value = key
    if (key !== 'Efectivo') amountReceived.value = 0
    showPaymentModal.value = false
  }

  // ── Cart actions ──────────────────────────────────────────────────────────

  /** Deactivation always blocks a sale, regardless of `validate_stock` -
   * that toggle is specifically about stock quantity, not about whether the
   * product is sellable at all. Mirrors ProductCard.vue's own
   * `unavailableReason` precedence (deactivated beats out-of-stock). */
  function unavailableReason(product: Product): string | null {
    if (!product.isActive) return `"${product.productName}" está desactivado`
    if (config.defaults.validateStock) {
      const inCart = cartItems.value.find(i => i.product.id === product.id)?.quantity ?? 0
      if (inCart >= product.stockQuantity) return `Sin stock suficiente de "${product.productName}"`
    }
    return null
  }

  function addProduct(product: Product) {
    const reason = unavailableReason(product)
    if (reason) {
      showToast(reason, 'error')
      return
    }
    const existing = cartItems.value.find(i => i.product.id === product.id)
    if (existing) {
      existing.quantity++
    } else {
      cartItems.value.push({ product, quantity: 1, unitPrice: product.unitPrice })
    }
  }

  function increaseQty(item: CartItem) {
    const reason = unavailableReason(item.product)
    if (reason) {
      showToast(reason, 'error')
      return
    }
    item.quantity++
  }

  function decreaseQty(item: CartItem) {
    if (item.quantity > 1) item.quantity--
    else removeItem(item)
  }

  function removeItem(item: CartItem) {
    const idx = cartItems.value.indexOf(item)
    if (idx >= 0) cartItems.value.splice(idx, 1)
  }

  function newSale() {
    cartItems.value  = []
    customerName.value = ''
    discount.value   = 0
    amountReceived.value = 0
    paymentMethod.value = resolveDefaultPaymentMethod()
    activePendingSale.value = null
  }

  // ── Save modal ────────────────────────────────────────────────────────────

  /** Sale already parked once (resumed from `pendingSales`) — update that
   * same row straight away instead of asking for a name again. */
  function requestSave() {
    if (cartItems.value.length === 0) return
    if (activePendingSale.value) {
      confirmSave(activePendingSale.value.label, activePendingSale.value.iconKey)
      return
    }
    showSaveModal.value = true
  }

  function cancelSave() {
    showSaveModal.value = false
  }

  async function confirmSave(label: string, iconKey: string) {
    const id = activePendingSale.value?.id ?? `ps-${Date.now()}`
    const sale: PendingSale = {
      id,
      label,
      iconKey,
      items:    [...cartItems.value],
      subtotal: subtotal.value,
      discount: discount.value,
      total:    total.value,
      createdAt: new Date(),
    }
    try {
      await invoke('save_pending_sale', {
        id,
        label,
        iconKey,
        itemsJson: JSON.stringify(cartItems.value),
        subtotal:  subtotal.value,
        discount:  discount.value,
        total:     total.value,
      })
    } catch (e) {
      console.error('[pending-sales] save failed:', e)
    }
    pendingSales.value = pendingSales.value.filter(s => s.id !== id)
    pendingSales.value.unshift(sale)
    showSaveModal.value = false
    newSale()
  }

  // ── Pending sales ─────────────────────────────────────────────────────────

  async function loadPendingSales() {
    try {
      const rows = await invoke<{
        id: string; label: string; iconKey: string; itemsJson: string
        subtotal: number; discount: number; total: number; createdAt: string
      }[]>('get_pending_sales')

      pendingSales.value = rows.map(row => ({
        id:        row.id,
        label:     row.label,
        iconKey:   row.iconKey,
        items:     JSON.parse(row.itemsJson) as CartItem[],
        subtotal:  row.subtotal,
        discount:  row.discount,
        total:     row.total,
        createdAt: new Date(row.createdAt),
      }))
    } catch (e) {
      console.error('[pending-sales] load failed, using in-memory fallback:', e)
    }
  }

  async function resumeSale(sale: PendingSale) {
    cartItems.value    = [...sale.items]
    customerName.value = sale.label
    discount.value     = sale.discount
    amountReceived.value = 0
    activePendingSale.value = { id: sale.id, label: sale.label, iconKey: sale.iconKey }
    try {
      await invoke('delete_pending_sale', { id: sale.id })
    } catch (e) {
      console.error('[pending-sales] delete failed:', e)
    }
    pendingSales.value = pendingSales.value.filter(s => s.id !== sale.id)
  }

  onMounted(loadPendingSales)
  onMounted(loadAndDefaultPaymentMethod)

  return {
    customerName,
    cartItems,
    pendingSales,
    showSaveModal,
    subtotal,
    discount,
    total,
    amountReceived,
    changeDue,
    paymentMethods,
    paymentMethod,
    paymentMethodId,
    showPaymentModal,
    canCheckout,
    openPaymentMethodModal,
    selectPaymentMethod,
    addProduct,
    increaseQty,
    decreaseQty,
    removeItem,
    requestSave,
    cancelSave,
    confirmSave,
    newSale,
    resumeSale,
  }
}

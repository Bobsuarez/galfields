<script setup lang="ts">
import { ref, computed, watch, nextTick, onMounted, onUnmounted } from 'vue'
import type { CartItem } from '../../../types'
import { formatCurrency } from '../../../utils/currency'
import { getPaymentMethodEmoji } from '../../../composables/usePaymentMethods'
import AppIcon from '../../../components/shared/AppIcon.vue'

const CATEGORY_EMOJI: Record<string, string> = {
  bebidas: '🥤', beverages: '🥤',
  comida: '🍽️',  food: '🍽️',
  snacks: '🍟',
  lacteos: '🥛', dairy: '🥛',
  limpieza: '🧹', cleaning: '🧹',
}

function categoryEmoji(category: string): string {
  return CATEGORY_EMOJI[category?.toLowerCase()] ?? '📦'
}

/** Shows the cash-received amount formatted like the rest of the panel (e.g. "$ 10.000"); empty until the user types something. */
function formatCashInput(value: number): string {
  return value > 0 ? formatCurrency(value) : ''
}

/** Strips everything but digits so pasted/typed "$", "." or spaces don't break the underlying number. */
function onCashInput(event: Event) {
  const digitsOnly = (event.target as HTMLInputElement).value.replace(/\D/g, '')
  emit('update:amountReceived', digitsOnly ? Number(digitsOnly) : 0)
}

const props = defineProps<{
  cartItems: CartItem[]
  subtotal: number
  discount: number
  total: number
  amountReceived: number
  changeDue: number
  paymentMethod: string
  canCheckout: boolean
}>()

const emit = defineEmits<{
  (e: 'update:amountReceived', value: number): void
  (e: 'edit-payment-method'): void
  (e: 'increase', item: CartItem): void
  (e: 'decrease', item: CartItem): void
  (e: 'remove', item: CartItem): void
  (e: 'save'): void
  (e: 'new'): void
  (e: 'checkout'): void
}>()

// Jumps the cashier straight to the cash-received field whenever an item is
// added (new product or +1 on an existing one), so a full sale can be typed
// without ever touching the mouse.
const cashInputRef = ref<HTMLInputElement | null>(null)
const sectionRef    = ref<HTMLElement | null>(null)
const totalQuantity = computed(() => props.cartItems.reduce((sum, item) => sum + item.quantity, 0))

watch(totalQuantity, async (newQty, oldQty) => {
  if (newQty > oldQty) {
    await nextTick()
    cashInputRef.value?.focus()
  }
})

// Enter checks out from anywhere in this panel (typically right after typing
// the cash amount). Listens on `window` instead of just this section because
// focus doesn't reliably stay inside the panel: picking a payment method in
// PaymentMethodModal removes the clicked button from the DOM, and the
// browser drops focus to <body> — a listener scoped to this section alone
// would silently miss the next Enter press since the event never bubbles
// into it from body. To avoid hijacking Enter from unrelated fields
// elsewhere (e.g. the product search box), only react when focus is either
// nowhere in particular (body) or actually inside this panel.
function onWindowKeydown(event: KeyboardEvent) {
  if (event.key !== 'Enter') return
  const target = event.target as HTMLElement | null
  if (target?.tagName === 'BUTTON') return // let the focused button handle its own Enter
  if (target && target !== document.body && !sectionRef.value?.contains(target)) return
  if (!props.canCheckout) return
  emit('checkout')
}

onMounted(() => window.addEventListener('keydown', onWindowKeydown))
onUnmounted(() => window.removeEventListener('keydown', onWindowKeydown))
</script>

<template>
  <section ref="sectionRef" class="active-sale">
    <div class="sale-header">
      <h2 class="sale-title">Venta Activa</h2>
      <button class="new-sale-btn" @click="emit('new')">
        <AppIcon name="plus" :size="13" />
        Nueva
      </button>
    </div>

    <button class="payment-method-field" type="button" @click="emit('edit-payment-method')">
      <span class="payment-method-emoji">{{ getPaymentMethodEmoji(paymentMethod) }}</span>
      <span class="payment-method-label">{{ paymentMethod }}</span>
      <AppIcon name="chevron-right" :size="13" class="payment-method-chevron" />
    </button>

    <div class="cart-list">
      <div v-if="cartItems.length === 0" class="cart-empty">
        <span style="font-size: 28px">🛒</span>
        <p>Agrega productos al carrito</p>
      </div>

      <div v-for="item in cartItems" :key="item.product.id" class="cart-item">
        <div class="item-emoji">{{ categoryEmoji(item.product.category) }}</div>
        <div class="item-info">
          <p class="item-name">{{ item.product.productName }}</p>
          <p class="item-unit">{{ formatCurrency(item.unitPrice) }} / u.</p>
        </div>
        <div class="item-qty">
          <button class="qty-btn" @click="emit('decrease', item)">
            <AppIcon name="minus" :size="11" />
          </button>
          <span class="qty-value">{{ item.quantity }}</span>
          <button class="qty-btn" @click="emit('increase', item)">
            <AppIcon name="plus" :size="11" />
          </button>
        </div>
        <div class="item-total">
          {{ formatCurrency(item.unitPrice * item.quantity) }}
        </div>
        <button class="remove-btn" @click="emit('remove', item)">
          <AppIcon name="x" :size="12" />
        </button>
      </div>
    </div>

    <div class="sale-summary">
      <div class="summary-row">
        <span class="summary-label">Subtotal</span>
        <span class="summary-value">{{ formatCurrency(subtotal) }}</span>
      </div>
      <div class="summary-row">
        <span class="summary-label">Descuento</span>
        <span class="summary-value discount">− {{ formatCurrency(discount) }}</span>
      </div>
      <div class="total-block">
        <span class="total-label">Total a cobrar</span>
        <span class="total-value">{{ formatCurrency(total) }}</span>
      </div>

      <div class="cash-calc" v-if="cartItems.length > 0 && paymentMethod === 'Efectivo'">
        <div class="cash-input-row">
          <label class="cash-label" for="cash-received">Efectivo recibido</label>
          <input
            id="cash-received"
            ref="cashInputRef"
            :value="formatCashInput(amountReceived)"
            @input="onCashInput"
            type="text"
            inputmode="numeric"
            placeholder="$ 0"
            class="cash-input"
          />
        </div>
        <div v-if="amountReceived > 0" class="change-row" :class="{ 'change-row--short': changeDue < 0 }">
          <span class="change-label">{{ changeDue >= 0 ? 'Cambio' : 'Falta' }}</span>
          <span class="change-value">{{ formatCurrency(Math.abs(changeDue)) }}</span>
        </div>
      </div>
    </div>

    <div class="sale-actions">
      <button
        class="btn btn-secondary"
        :disabled="cartItems.length === 0"
        @click="emit('save')"
      >
        <AppIcon name="save" :size="14" />
        Guardar Venta
      </button>
      <button
        class="btn btn-danger"
        :disabled="cartItems.length === 0"
        @click="emit('new')"
      >
        <AppIcon name="x" :size="14" />
        Limpiar
      </button>
    </div>

    <button
      class="btn btn-checkout"
      :disabled="!canCheckout"
      @click="emit('checkout')"
    >
      <AppIcon name="credit-card" :size="16" />
      Facturar / Cobrar
    </button>
  </section>
</template>

<style scoped>
.active-sale {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: var(--color-surface-2);
  border-left: 1px solid var(--color-border);
  overflow: hidden;
}

.sale-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 14px 8px;
  flex-shrink: 0;
  border-bottom: 1px solid var(--color-border);
}

.sale-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-cream);
}

.new-sale-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  background: rgba(242, 141, 53, 0.12);
  border: 1px solid rgba(242, 141, 53, 0.3);
  border-radius: var(--radius-sm);
  color: var(--color-primary);
  font-size: 11px;
  font-weight: 600;
  transition: background 0.15s;
}

.new-sale-btn:hover {
  background: rgba(242, 141, 53, 0.2);
}

.payment-method-field {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  border: none;
  border-bottom: 1px solid var(--color-border);
  background: transparent;
  flex-shrink: 0;
  cursor: pointer;
  transition: background 0.15s;
  font-family: inherit;
  text-align: left;
}

.payment-method-field:hover {
  background: rgba(242, 141, 53, 0.06);
}

.payment-method-emoji {
  font-size: 16px;
  line-height: 1;
  flex-shrink: 0;
}

.payment-method-label {
  flex: 1;
  color: var(--color-text);
  font-size: 13px;
  font-weight: 600;
}

.payment-method-chevron {
  color: var(--color-text-dim);
  flex-shrink: 0;
}

.cart-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px 0;
}

.cart-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  gap: 8px;
  color: var(--color-text-muted);
  font-size: 12px;
}

.cart-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 14px;
  border-bottom: 1px solid rgba(242, 141, 53, 0.06);
  transition: background 0.1s;
}

.cart-item:hover {
  background: rgba(242, 141, 53, 0.04);
}

.item-emoji {
  font-size: 22px;
  flex-shrink: 0;
  width: 28px;
  text-align: center;
}

.item-info {
  flex: 1;
  min-width: 0;
}

.item-name {
  font-size: 12px;
  font-weight: 500;
  color: var(--color-cream);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.item-unit {
  font-size: 10px;
  color: var(--color-text-muted);
}

.item-qty {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
}

.qty-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  background: var(--color-surface-3);
  border: 1px solid var(--color-border);
  border-radius: 4px;
  color: var(--color-text-muted);
  transition: all 0.15s;
}

.qty-btn:hover {
  background: var(--color-primary);
  border-color: var(--color-primary);
  color: #0D0D0D;
}

.qty-value {
  font-size: 13px;
  font-weight: 700;
  color: var(--color-cream);
  min-width: 20px;
  text-align: center;
  font-variant-numeric: tabular-nums;
}

.item-total {
  font-size: 12px;
  font-weight: 600;
  color: var(--color-primary);
  min-width: 56px;
  text-align: right;
  flex-shrink: 0;
}

.remove-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  background: transparent;
  border: none;
  color: var(--color-text-dim);
  border-radius: 4px;
  flex-shrink: 0;
  transition: color 0.15s, background 0.15s;
}

.remove-btn:hover {
  color: var(--color-danger);
  background: rgba(229, 57, 53, 0.1);
}

.sale-summary {
  padding: 10px 14px 0;
  border-top: 1px solid var(--color-border);
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.summary-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1px 0;
}

.summary-label {
  font-size: 10.5px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.4px;
  color: var(--color-text-dim);
}

.summary-value {
  font-size: 12.5px;
  color: var(--color-text-muted);
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}

.summary-value.discount { color: var(--color-success); }

/* ── Total — the star of the show ─────────────────────────── */
.total-block {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 1px;
  margin-top: 6px;
  padding: 10px 12px;
  background: linear-gradient(135deg, rgba(242,141,53,0.08) 0%, rgba(242,141,53,0.04) 100%);
  border: 1px solid rgba(242, 141, 53, 0.2);
  border-radius: var(--radius-md);
}

.total-label {
  font-size: 9.5px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 1px;
  color: var(--color-text-muted);
}

.total-value {
  font-size: 35px;
  font-weight: 800;
  color: var(--color-primary);
  letter-spacing: -1px;
  line-height: 1.1;
  font-variant-numeric: tabular-nums;
  text-shadow: 0 0 24px rgba(242, 141, 53, 0.25);
}

/* ── Cash calculator — "efectivo recibido" / change ────────── */
.cash-calc {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-top: 6px;
}

.cash-input-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 8px 10px;
  background: var(--color-surface-3);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
}

.cash-label {
  font-size: 10.5px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.4px;
  color: var(--color-text-dim);
  white-space: nowrap;
}

.cash-input {
  flex: 1;
  min-width: 0;
  background: transparent;
  border: none;
  color: var(--color-cream);
  font-size: 25px;
  font-weight: 700;
  text-align: right;
  font-variant-numeric: tabular-nums;
}

.cash-input::placeholder {
  color: var(--color-text-dim);
  font-weight: 500;
}

.cash-input:focus { outline: none; }

.change-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  background: rgba(76, 175, 80, 0.1);
  border: 1px solid rgba(76, 175, 80, 0.3);
  border-radius: var(--radius-sm);
}

.change-row--short {
  background: rgba(229, 57, 53, 0.1);
  border-color: rgba(229, 57, 53, 0.3);
}

.change-label {
  font-size: 10.5px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.4px;
  color: var(--color-success);
}

.change-row--short .change-label { color: var(--color-danger); }

.change-value {
  font-size: 25px;
  font-weight: 800;
  color: var(--color-success);
  font-variant-numeric: tabular-nums;
}

.change-row--short .change-value { color: var(--color-danger); }

.sale-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  padding: 10px 14px 6px;
  flex-shrink: 0;
}

.btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 8px 12px;
  border-radius: var(--radius-sm);
  font-size: 12px;
  font-weight: 600;
  transition: all 0.15s;
  cursor: pointer;
  border: none;
}

.btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.btn-secondary {
  background: var(--color-surface-3);
  border: 1px solid var(--color-border);
  color: var(--color-cream);
}

.btn-secondary:hover:not(:disabled) {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.btn-danger {
  background: rgba(229, 57, 53, 0.1);
  border: 1px solid rgba(229, 57, 53, 0.3);
  color: var(--color-danger);
}

.btn-danger:hover:not(:disabled) {
  background: rgba(229, 57, 53, 0.2);
}

.btn-checkout {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: calc(100% - 28px);
  margin: 0 14px 14px;
  padding: 12px;
  background: var(--color-primary);
  border: none;
  border-radius: var(--radius-md);
  color: #0D0D0D;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  transition: background 0.15s, transform 0.1s;
  flex-shrink: 0;
}

.btn-checkout:hover:not(:disabled) {
  background: var(--color-primary-hover);
  transform: translateY(-1px);
}

.btn-checkout:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
</style>

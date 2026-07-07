<script setup lang="ts">
import { ref, watch, onUnmounted } from 'vue'
import type { CartItem, ConfigSettings } from '../../../types'
import InvoiceDocument from './InvoiceDocument.vue'

const props = defineProps<{
  visible: boolean
  isProcessing: boolean
  store: ConfigSettings['store']
  invoiceNumber: string
  date: string
  seller: string
  customer: string
  items: CartItem[]
  subtotal: number
  discount: number
  total: number
  paymentMethod: string
  amountReceived: number
  changeDue: number
  printReceipt: boolean
}>()

const emit = defineEmits<{
  (e: 'confirm'): void
  (e: 'cancel'): void
}>()

const docRef = ref<InstanceType<typeof InvoiceDocument> | null>(null)

// html2canvas respects ancestor `overflow`/scroll clipping — capturing `docRef`
// directly would crop the PDF to whatever fits inside the scrollable
// `.modal-body`. `captureRef` is an off-screen, unclipped twin of the same
// document used only as the capture source, so the PDF always contains the
// full invoice regardless of the on-screen preview's scroll position.
const captureRef = ref<InstanceType<typeof InvoiceDocument> | null>(null)
defineExpose({ getElement: () => captureRef.value?.rootEl ?? null })

// Enter confirms the sale while the modal is open — but not if the focused
// element is itself a button (Cancelar/Confirmar already handle their own
// Enter natively; re-triggering confirm on top of that would fire both
// actions at once), and not while a previous confirm is still processing.
function onKeydown(event: KeyboardEvent) {
  if (event.key !== 'Enter') return
  if ((event.target as HTMLElement | null)?.tagName === 'BUTTON') return
  if (props.isProcessing) return
  emit('confirm')
}

// The Enter that opened this modal (from ActiveSale's own Enter-to-checkout)
// can still be "in flight" as an OS key-repeat if held a beat too long, and
// would otherwise reach this modal's listener the instant it's attached and
// confirm it immediately — skipping the review step entirely. Opening and
// confirming must be two genuinely separate keypresses, so the listener
// only attaches after a short grace period once the modal is visible.
let enterGuardTimer: ReturnType<typeof setTimeout> | null = null

function clearEnterGuard() {
  if (enterGuardTimer) {
    clearTimeout(enterGuardTimer)
    enterGuardTimer = null
  }
}

watch(() => props.visible, visible => {
  clearEnterGuard()
  if (visible) {
    enterGuardTimer = setTimeout(() => window.addEventListener('keydown', onKeydown), 350)
  } else {
    window.removeEventListener('keydown', onKeydown)
  }
})

onUnmounted(() => {
  clearEnterGuard()
  window.removeEventListener('keydown', onKeydown)
})
</script>

<template>
  <Transition name="modal">
    <div v-if="visible" class="modal-backdrop" @click.self="!isProcessing && emit('cancel')">
      <div class="modal-card">
        <div class="modal-header">
          <span class="modal-icon">🧾</span>
          <h2 class="modal-title">Facturar / Cobrar</h2>
        </div>

        <div class="modal-body">
          <InvoiceDocument
            ref="docRef"
            :store="store"
            :invoice-number="invoiceNumber"
            :date="date"
            :seller="seller"
            :customer="customer"
            :items="items"
            :subtotal="subtotal"
            :discount="discount"
            :total="total"
            :payment-method="paymentMethod"
            :amount-received="amountReceived"
            :change-due="changeDue"
          />
        </div>

        <div class="modal-footer">
          <span class="footer-hint">
            {{ printReceipt ? '🖨️ Se enviará a la impresora' : '📄 Se guardará como PDF' }}
          </span>
          <div class="footer-actions">
            <button class="btn-cancel" :disabled="isProcessing" @click="emit('cancel')">
              Cancelar
            </button>
            <button class="btn-confirm" :disabled="isProcessing" @click="emit('confirm')">
              {{ isProcessing ? 'Procesando…' : 'Confirmar' }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </Transition>

  <div v-if="visible" class="capture-clone" aria-hidden="true">
    <InvoiceDocument
      ref="captureRef"
      :store="store"
      :invoice-number="invoiceNumber"
      :date="date"
      :seller="seller"
      :customer="customer"
      :items="items"
      :subtotal="subtotal"
      :discount="discount"
      :total="total"
      :payment-method="paymentMethod"
      :amount-received="amountReceived"
      :change-due="changeDue"
    />
  </div>
</template>

<style scoped>
.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.72);
  z-index: 1000;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  backdrop-filter: blur(2px);
  overflow-y: auto;
  padding: 24px 0 0;
}

.modal-card {
  background: var(--color-surface-2);
  border: 1px solid var(--color-border-strong);
  border-radius: var(--radius-lg);
  max-width: calc(100vw - 32px);
  box-shadow: 0 24px 64px rgba(0, 0, 0, 0.6), 0 0 0 1px rgba(242, 141, 53, 0.08);
  /* No `overflow: hidden` here — that would make this card (which never
     scrolls internally) the sticky positioning container for the header/
     footer below, instead of `.modal-backdrop`, silently breaking `sticky`.
     Corners are rounded on the header/footer themselves instead. */
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  margin: auto;
}

.modal-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 18px 20px 14px;
  border-bottom: 1px solid var(--color-border);
  flex-shrink: 0;
  border-radius: var(--radius-lg) var(--radius-lg) 0 0;
  /* Stays pinned to the top of the (page-level) scroll while the invoice
     scrolls underneath, so it never gets pushed off-screen. */
  position: sticky;
  top: 0;
  z-index: 1;
  background: var(--color-surface-2);
}

.modal-icon {
  font-size: 20px;
  line-height: 1;
}

.modal-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--color-cream);
}

.modal-body {
  padding: 18px 20px;
  display: flex;
  justify-content: center;
}

.modal-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 20px;
  border-top: 1px solid var(--color-border);
  flex-shrink: 0;
  border-radius: 0 0 var(--radius-lg) var(--radius-lg);
  /* Stays pinned to the bottom of the (page-level) scroll — Cancelar/Confirmar
     are always reachable without scrolling through the whole invoice. */
  position: sticky;
  bottom: 0px;
  z-index: 1;
  background: var(--color-surface-2);
}

.footer-hint {
  font-size: 11px;
  color: var(--color-text-muted);
}

.footer-actions {
  display: flex;
  gap: 8px;
}

.btn-cancel {
  padding: 8px 16px;
  background: transparent;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  color: var(--color-text-muted);
  font-size: 12.5px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
  font-family: inherit;
}

.btn-cancel:hover:not(:disabled) {
  border-color: var(--color-text-muted);
  color: var(--color-cream);
}

.btn-confirm {
  padding: 8px 20px;
  background: var(--color-primary);
  border: none;
  border-radius: var(--radius-sm);
  color: #0D0D0D;
  font-size: 12.5px;
  font-weight: 700;
  cursor: pointer;
  transition: background 0.15s, transform 0.1s;
  font-family: inherit;
}

.btn-confirm:hover:not(:disabled) {
  background: var(--color-primary-hover);
  transform: translateY(-1px);
}

.btn-cancel:disabled,
.btn-confirm:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

/* Off-screen capture twin — laid out normally (no clipping/scroll ancestors)
   so html2canvas always sees the invoice's full height. */
.capture-clone {
  position: fixed;
  top: 0;
  left: -10000px;
  pointer-events: none;
}

/* Transition */
.modal-enter-active { animation: modal-in  0.2s ease-out; }
.modal-leave-active { animation: modal-out 0.15s ease-in forwards; }

@keyframes modal-in  {
  from { opacity: 0; transform: scale(0.93) translateY(8px); }
  to   { opacity: 1; transform: scale(1)    translateY(0);   }
}
@keyframes modal-out {
  from { opacity: 1; transform: scale(1)    translateY(0);   }
  to   { opacity: 0; transform: scale(0.95) translateY(4px); }
}
</style>

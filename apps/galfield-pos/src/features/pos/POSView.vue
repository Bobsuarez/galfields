<script setup lang="ts">
import { useActiveSale }    from './composables/useActiveSale'
import { useBarcodeScanner } from './composables/useBarcodeScanner'
import { useCheckout }      from '../invoices/composables/useCheckout'
import { useAppConfig }     from '../../composables/useAppConfig'
import type { Product }     from '../../types'
import ProductCatalog    from './components/ProductCatalog.vue'
import ActiveSale        from './components/ActiveSale.vue'
import PendingSales      from './components/PendingSales.vue'
import SaveSaleModal     from './components/SaveSaleModal.vue'
import PaymentMethodModal from './components/PaymentMethodModal.vue'
import InvoiceModal      from '../invoices/components/InvoiceModal.vue'

const {
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
} = useActiveSale()

// Barcode scanner: starts when POS mounts, stops when it unmounts.
// Scanned barcodes that match a product go straight to the cart.
const { isConnected: barcodeConnected, activePort: barcodePort } =
  useBarcodeScanner(addProduct)

function onProductAdded(product: Product) {
  addProduct(product)
}

const { config } = useAppConfig()

const {
  showModal:    showInvoiceModal,
  isProcessing: isCheckoutProcessing,
  invoiceNumber,
  invoiceDate,
  modalRef:     invoiceModalRef,
  requestCheckout,
  cancelCheckout,
  confirmCheckout,
} = useCheckout(newSale)

function onCheckout() {
  requestCheckout({
    customerName:   customerName.value,
    cartItems:      cartItems.value,
    subtotal:       subtotal.value,
    discount:       discount.value,
    total:          total.value,
    amountReceived: amountReceived.value,
    paymentMethod:  paymentMethod.value,
    paymentMethodId: paymentMethodId.value ?? 0,
  })
}
</script>

<template>
  <div class="pos-view">

    <!-- Barcode scanner status pill (only shown when a port is configured) -->
    <div v-if="barcodePort" class="scanner-status" :class="barcodeConnected ? 'scanner-status--ok' : 'scanner-status--off'">
      <span class="scanner-dot"></span>
      <span class="scanner-label">
        {{ barcodeConnected ? `Lector activo — ${barcodePort}` : `Lector inactivo — ${barcodePort}` }}
      </span>
    </div>

    <div class="pos-main">
      <ProductCatalog @product-added="onProductAdded" />
      <ActiveSale
        :cart-items="cartItems"
        :subtotal="subtotal"
        :discount="discount"
        :total="total"
        :amount-received="amountReceived"
        :change-due="changeDue"
        :payment-method="paymentMethod"
        :can-checkout="canCheckout"
        @update:amount-received="amountReceived = $event"
        @edit-payment-method="openPaymentMethodModal"
        @increase="increaseQty"
        @decrease="decreaseQty"
        @remove="removeItem"
        @save="requestSave"
        @new="newSale"
        @checkout="onCheckout"
      />
    </div>
    <PendingSales :sales="pendingSales" @resume="resumeSale" />
  </div>

  <SaveSaleModal
    :visible="showSaveModal"
    @confirm="confirmSave"
    @cancel="cancelSave"
  />

  <PaymentMethodModal
    :visible="showPaymentModal"
    :current="paymentMethod"
    :methods="paymentMethods"
    @select="selectPaymentMethod"
    @cancel="showPaymentModal = false"
  />

  <InvoiceModal
    ref="invoiceModalRef"
    :visible="showInvoiceModal"
    :is-processing="isCheckoutProcessing"
    :store="config.store"
    :invoice-number="invoiceNumber"
    :date="invoiceDate"
    :seller="config.defaults.seller"
    :customer="customerName || config.defaults.customer"
    :items="cartItems"
    :subtotal="subtotal"
    :discount="discount"
    :total="total"
    :payment-method="paymentMethod"
    :amount-received="amountReceived"
    :change-due="changeDue"
    :print-receipt="config.defaults.printReceipt"
    @confirm="confirmCheckout"
    @cancel="cancelCheckout"
  />
</template>

<style scoped>
.pos-view {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

/* ── Scanner status ──────────────────────────────────────── */
.scanner-status {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 12px;
  font-size: 10.5px;
  font-weight: 600;
  flex-shrink: 0;
  border-bottom: 1px solid var(--color-border);
}

.scanner-status--ok  { background: rgba(56, 142, 60, 0.08);  color: #81c784; }
.scanner-status--off { background: rgba(229, 57, 53, 0.06);  color: #e57373; }

.scanner-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
}

.scanner-status--ok  .scanner-dot { background: #81c784; box-shadow: 0 0 0 3px rgba(129,199,132,0.2); animation: dot-pulse 2s ease-in-out infinite; }
.scanner-status--off .scanner-dot { background: #e57373; }

@keyframes dot-pulse {
  0%, 100% { box-shadow: 0 0 0 2px rgba(129, 199, 132, 0.3); }
  50%       { box-shadow: 0 0 0 5px rgba(129, 199, 132, 0);   }
}

/* ── Layout ──────────────────────────────────────────────── */
.pos-main {
  flex: 1;
  display: flex;
  overflow: hidden;
  min-height: 0;
}

.pos-main > :first-child {
  flex: 1;
  min-width: 0;
}

.pos-main > :last-child {
  width: 440px;
  flex-shrink: 0;
}
</style>

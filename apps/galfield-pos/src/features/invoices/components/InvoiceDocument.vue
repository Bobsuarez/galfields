<script setup lang="ts">
import { ref } from 'vue'
import type { CartItem, ConfigSettings } from '../../../types'
import { formatCurrency } from '../../../utils/currency'

defineProps<{
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
}>()

const rootEl = ref<HTMLElement | null>(null)
defineExpose({ rootEl })
</script>

<template>
  <div ref="rootEl" class="invoice-doc">
    <header class="invoice-header">
      <h1 class="store-name">{{ store.name }}</h1>
      <p v-if="store.slogan" class="store-slogan">{{ store.slogan }}</p>
      <p class="store-line">{{ store.address }}</p>
      <p class="store-line">{{ store.phone }} · {{ store.email }}</p>
    </header>

    <div class="invoice-meta">
      <div class="meta-block">
        <span class="meta-label">Factura</span>
        <span class="meta-value meta-value--accent">{{ invoiceNumber }}</span>
      </div>
      <div class="meta-block">
        <span class="meta-label">Fecha</span>
        <span class="meta-value">{{ date }}</span>
      </div>
    </div>

    <div class="invoice-parties">
      <div class="party-block">
        <span class="meta-label">Cliente</span>
        <span class="meta-value">{{ customer }}</span>
      </div>
      <div class="party-block">
        <span class="meta-label">Vendedor</span>
        <span class="meta-value">{{ seller }}</span>
      </div>
    </div>

    <table class="items-table">
      <thead>
        <tr>
          <th class="col-name">Producto</th>
          <th class="col-qty">Cant.</th>
          <th class="col-price">Precio</th>
          <th class="col-total">Subtotal</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="item in items" :key="item.product.id">
          <td class="col-name">{{ item.product.productName }}</td>
          <td class="col-qty">{{ item.quantity }}</td>
          <td class="col-price">{{ formatCurrency(item.unitPrice) }}</td>
          <td class="col-total">{{ formatCurrency(item.unitPrice * item.quantity) }}</td>
        </tr>
      </tbody>
    </table>

    <div class="invoice-totals">
      <div class="totals-row">
        <span>Subtotal</span>
        <span>{{ formatCurrency(subtotal) }}</span>
      </div>
      <div class="totals-row">
        <span>Descuento</span>
        <span>− {{ formatCurrency(discount) }}</span>
      </div>
      <div class="totals-row totals-row--grand">
        <span>Total</span>
        <span>{{ formatCurrency(total) }}</span>
      </div>
      <div class="totals-row totals-row--payment">
        <span>Método de pago</span>
        <span>{{ paymentMethod }}</span>
      </div>
      <template v-if="amountReceived > 0">
        <div class="totals-row totals-row--payment">
          <span>Efectivo recibido</span>
          <span>{{ formatCurrency(amountReceived) }}</span>
        </div>
        <div class="totals-row totals-row--change">
          <span>Cambio</span>
          <span>{{ formatCurrency(changeDue) }}</span>
        </div>
      </template>
    </div>

    <footer class="invoice-footer">
      <p>¡Gracias por tu compra!</p>
    </footer>
  </div>
</template>

<style scoped>
/*
  Fixed light palette regardless of the app's dark theme — this document is
  meant to be printed / archived as a PDF, so it must stay legible on paper.
*/
.invoice-doc {
  width: 380px;
  background: #FDF9F0;
  color: #2A1A0D;
  font-family: 'Courier New', monospace;
  padding: 24px 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.invoice-header {
  text-align: center;
  border-bottom: 2px dashed #8C501B;
  padding-bottom: 14px;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.store-name {
  font-size: 19px;
  font-weight: 800;
  color: #F28D35;
  letter-spacing: 0.5px;
}

.store-slogan {
  font-size: 11px;
  font-style: italic;
  color: #8C501B;
}

.store-line {
  font-size: 10.5px;
  color: #5A4230;
}

.invoice-meta,
.invoice-parties {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.meta-block,
.party-block {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.meta-label {
  font-size: 9px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  color: #8C501B;
}

.meta-value {
  font-size: 12.5px;
  font-weight: 600;
  color: #2A1A0D;
}

.meta-value--accent {
  color: #F28D35;
  font-weight: 800;
}

.items-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 11px;
}

.items-table thead th {
  border-bottom: 1px solid #8C501B;
  padding: 4px 2px;
  text-align: left;
  font-size: 9.5px;
  text-transform: uppercase;
  letter-spacing: 0.4px;
  color: #8C501B;
}

.items-table td {
  padding: 5px 2px;
  border-bottom: 1px dotted #E0C9A6;
}

.col-qty,
.col-price,
.col-total {
  text-align: right;
  white-space: nowrap;
}

.invoice-totals {
  border-top: 2px dashed #8C501B;
  padding-top: 10px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.totals-row {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #5A4230;
}

.totals-row--grand {
  font-size: 16px;
  font-weight: 800;
  color: #F28D35;
  padding-top: 4px;
}

.totals-row--payment {
  font-size: 10.5px;
  color: #8C501B;
}

.totals-row--change {
  font-size: 13px;
  font-weight: 800;
  color: #2E7D32;
  padding-top: 2px;
}

.invoice-footer {
  text-align: center;
  font-size: 11px;
  color: #8C501B;
  padding-top: 4px;
}
</style>

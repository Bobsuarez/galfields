<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useInvoiceHistory } from '../composables/useInvoiceHistory'
import { formatCurrency } from '../../../utils/currency'
import AppIcon from '../../../components/shared/AppIcon.vue'

const {
  sales, isLoading, searchQuery,
  selected, isLoadingDetail, isCancelling, detailError,
  loadSales, openSale, closeSale, cancelSale,
} = useInvoiceHistory()

onMounted(loadSales)

let searchDebounce: ReturnType<typeof setTimeout> | undefined
watch(searchQuery, () => {
  clearTimeout(searchDebounce)
  searchDebounce = setTimeout(loadSales, 250)
})

const confirmingCancel = ref(false)

function openRow(saleId: number) {
  confirmingCancel.value = false
  openSale(saleId)
}

function close() {
  confirmingCancel.value = false
  closeSale()
}

async function confirmCancel() {
  if (!selected.value) return
  const ok = await cancelSale(selected.value.saleId)
  if (ok) confirmingCancel.value = false
}

function formatDateTime(value: string) {
  return new Date(value.replace(' ', 'T')).toLocaleString('es-CO', { dateStyle: 'medium', timeStyle: 'short' })
}
</script>

<template>
  <div class="history-view">
    <div class="history-header">
      <div>
        <h1 class="history-title">Facturas</h1>
        <p class="history-subtitle">Historial de ventas — cancela una factura si hace falta</p>
      </div>
      <div class="history-search">
        <AppIcon name="search" :size="13" class="search-icon" />
        <input v-model="searchQuery" type="text" placeholder="Buscar por número de factura..." class="search-input" />
      </div>
    </div>

    <div class="history-body">
      <table class="history-table">
        <thead>
          <tr>
            <th class="col-invoice">Factura</th>
            <th class="col-date">Fecha</th>
            <th class="col-method">Método de pago</th>
            <th class="col-total">Total</th>
            <th class="col-status">Estado</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="isLoading">
            <td colspan="5" class="empty-row">Cargando…</td>
          </tr>
          <template v-else>
            <tr
              v-for="sale in sales"
              :key="sale.saleId"
              class="table-row"
              :class="{ 'table-row--cancelled': sale.cancelledAt }"
              @click="openRow(sale.saleId)"
            >
              <td class="col-invoice">{{ sale.invoiceNumber }}</td>
              <td class="col-date">{{ formatDateTime(sale.createdAt) }}</td>
              <td class="col-method">{{ sale.paymentMethodName }}</td>
              <td class="col-total">{{ formatCurrency(sale.total) }}</td>
              <td class="col-status">
                <span v-if="sale.cancelledAt" class="status-badge status--cancelled">Cancelada</span>
                <span v-else class="status-badge status--active">Activa</span>
              </td>
            </tr>
            <tr v-if="sales.length === 0">
              <td colspan="5" class="empty-row">
                <AppIcon name="search" :size="20" />
                <span>Sin resultados</span>
              </td>
            </tr>
          </template>
        </tbody>
      </table>
    </div>

    <!-- Detail / cancel modal — same chrome as SaveSaleModal.vue -->
    <Transition name="modal">
      <div v-if="selected || isLoadingDetail" class="modal-backdrop" @click.self="close">
        <div class="modal-card">
          <div class="modal-header">
            <span class="modal-icon">🧾</span>
            <h2 class="modal-title">{{ selected ? `Factura ${selected.invoiceNumber}` : 'Cargando…' }}</h2>
          </div>

          <div class="modal-body">
            <template v-if="selected">
              <span v-if="selected.cancelledAt" class="status-badge status--cancelled">Cancelada el {{ formatDateTime(selected.cancelledAt) }}</span>

              <div class="detail-meta">
                <span>{{ formatDateTime(selected.createdAt) }}</span>
                <span>{{ selected.paymentMethodName }}</span>
              </div>

              <div class="detail-items">
                <div v-for="(item, i) in selected.items" :key="i" class="detail-item">
                  <div class="detail-item-info">
                    <span class="detail-item-name">{{ item.productName }}</span>
                    <span class="detail-item-meta">{{ item.quantity }} × {{ formatCurrency(item.unitPrice) }}</span>
                  </div>
                  <span class="detail-item-subtotal">{{ formatCurrency(item.subtotal) }}</span>
                </div>
              </div>

              <div class="detail-totals">
                <div class="detail-total-row"><span>Subtotal</span><span>{{ formatCurrency(selected.subtotal) }}</span></div>
                <div v-if="selected.discount > 0" class="detail-total-row"><span>Descuento</span><span>-{{ formatCurrency(selected.discount) }}</span></div>
                <div class="detail-total-row detail-total-row--final"><span>Total</span><span>{{ formatCurrency(selected.total) }}</span></div>
              </div>

              <p v-if="detailError" class="detail-error">{{ detailError }}</p>

              <div v-if="confirmingCancel" class="cancel-confirm">
                <p class="cancel-confirm-text">¿Seguro? Esto repone el stock descontado y no se puede deshacer.</p>
              </div>
            </template>
            <template v-else>
              <p class="detail-loading">Cargando factura…</p>
            </template>
          </div>

          <div class="modal-footer">
            <template v-if="selected && !selected.cancelledAt && confirmingCancel">
              <button class="btn-cancel" :disabled="isCancelling" @click="confirmingCancel = false">No, volver</button>
              <button class="btn-danger" :disabled="isCancelling" @click="confirmCancel">
                {{ isCancelling ? 'Cancelando…' : 'Sí, cancelar factura' }}
              </button>
            </template>
            <template v-else>
              <button class="btn-cancel" @click="close">Cerrar</button>
              <button v-if="selected && !selected.cancelledAt" class="btn-danger" @click="confirmingCancel = true">
                Cancelar factura
              </button>
            </template>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<style scoped>
.history-view {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
  background: var(--color-surface);
}

.history-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 16px;
  border-bottom: 1px solid var(--color-border);
  flex-shrink: 0;
}

.history-title {
  font-size: 18px;
  font-weight: 700;
  color: var(--color-cream);
  line-height: 1.2;
}

.history-subtitle {
  font-size: 12px;
  color: var(--color-text-muted);
  margin-top: 2px;
}

.history-search {
  display: flex;
  align-items: center;
  background: var(--color-surface-2);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  padding: 7px 12px;
  gap: 8px;
  width: 280px;
  transition: border-color 0.2s;
}

.history-search:focus-within { border-color: var(--color-primary); }
.search-icon { color: var(--color-text-muted); }
.search-input { flex: 1; background: transparent; border: none; color: var(--color-text); font-size: 12.5px; }
.search-input::placeholder { color: var(--color-text-dim); }

.history-body {
  flex: 1;
  overflow: auto;
  padding: 0 16px;
}

.history-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 12.5px;
}

.history-table th {
  padding: 8px 10px;
  text-align: left;
  color: var(--color-text-muted);
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.4px;
  border-bottom: 1px solid var(--color-border);
  position: sticky;
  top: 0;
  background: var(--color-surface);
}

.table-row {
  cursor: pointer;
  transition: background 0.1s;
  border-bottom: 1px solid rgba(242, 141, 53, 0.06);
}

.table-row:hover { background: rgba(242, 141, 53, 0.05); }
.table-row--cancelled { opacity: 0.55; }

.history-table td { padding: 9px 10px; vertical-align: middle; }

.col-invoice { font-weight: 600; color: var(--color-cream); }
.col-total { text-align: right; color: var(--color-primary); font-weight: 600; }
.col-status { width: 100px; }

.status-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 10.5px;
  font-weight: 600;
}

.status--active { background: rgba(76, 175, 80, 0.15); color: #81C784; }
.status--cancelled { background: rgba(229, 57, 53, 0.15); color: #EF5350; }

.empty-row {
  text-align: center;
  padding: 40px;
  color: var(--color-text-muted);
}

/* ── Detail / cancel modal — same chrome as SaveSaleModal.vue ─────────── */
.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.72);
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  backdrop-filter: blur(2px);
}

.modal-card {
  background: var(--color-surface-2);
  border: 1px solid var(--color-border-strong);
  border-radius: var(--radius-lg);
  width: 420px;
  max-width: calc(100vw - 32px);
  max-height: calc(100vh - 64px);
  box-shadow: 0 24px 64px rgba(0, 0, 0, 0.6), 0 0 0 1px rgba(242, 141, 53, 0.08);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.modal-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 18px 20px 14px;
  border-bottom: 1px solid var(--color-border);
  flex-shrink: 0;
}

.modal-icon { font-size: 20px; line-height: 1; }
.modal-title { font-size: 15px; font-weight: 700; color: var(--color-cream); }

.modal-body {
  padding: 18px 20px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  overflow-y: auto;
}

.detail-loading { color: var(--color-text-muted); font-size: 13px; text-align: center; padding: 20px 0; }

.detail-meta {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: var(--color-text-muted);
}

.detail-items {
  display: flex;
  flex-direction: column;
  gap: 8px;
  border-top: 1px solid var(--color-border);
  border-bottom: 1px solid var(--color-border);
  padding: 10px 0;
}

.detail-item { display: flex; justify-content: space-between; align-items: center; gap: 8px; }
.detail-item-info { display: flex; flex-direction: column; gap: 2px; }
.detail-item-name { font-size: 12.5px; font-weight: 500; color: var(--color-cream); }
.detail-item-meta { font-size: 11px; color: var(--color-text-muted); }
.detail-item-subtotal { font-size: 12.5px; font-weight: 600; color: var(--color-text); }

.detail-totals { display: flex; flex-direction: column; gap: 4px; }
.detail-total-row { display: flex; justify-content: space-between; font-size: 12.5px; color: var(--color-text-muted); }
.detail-total-row--final { font-size: 14px; font-weight: 700; color: var(--color-primary); }

.detail-error {
  font-size: 12px;
  color: #EF5350;
  background: rgba(229, 57, 53, 0.1);
  border-radius: var(--radius-sm);
  padding: 8px 10px;
}

.cancel-confirm {
  background: rgba(229, 57, 53, 0.1);
  border: 1px solid rgba(229, 57, 53, 0.3);
  border-radius: var(--radius-sm);
  padding: 10px 12px;
}

.cancel-confirm-text { font-size: 12.5px; color: #EF5350; }

.modal-footer {
  display: flex;
  gap: 8px;
  padding: 14px 20px;
  border-top: 1px solid var(--color-border);
  justify-content: flex-end;
  flex-shrink: 0;
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

.btn-cancel:hover { border-color: var(--color-text-muted); color: var(--color-cream); }

.btn-danger {
  padding: 8px 20px;
  background: #E53935;
  border: none;
  border-radius: var(--radius-sm);
  color: #fff;
  font-size: 12.5px;
  font-weight: 700;
  cursor: pointer;
  transition: background 0.15s, transform 0.1s;
  font-family: inherit;
}

.btn-danger:hover:not(:disabled) { background: #C62828; transform: translateY(-1px); }
.btn-danger:disabled { opacity: 0.5; cursor: not-allowed; }

.modal-enter-active { animation: modal-in 0.2s ease-out; }
.modal-leave-active { animation: modal-out 0.15s ease-in forwards; }

@keyframes modal-in {
  from { opacity: 0; transform: scale(0.93) translateY(8px); }
  to   { opacity: 1; transform: scale(1)    translateY(0);   }
}
@keyframes modal-out {
  from { opacity: 1; transform: scale(1)    translateY(0);   }
  to   { opacity: 0; transform: scale(0.95) translateY(4px); }
}
</style>

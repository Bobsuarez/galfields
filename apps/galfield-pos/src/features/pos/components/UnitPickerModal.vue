<script setup lang="ts">
import type { Product } from '../../../types'
import { formatCurrency } from '../../../utils/currency'
import { productUnavailableReason } from '../../../utils/stock'

defineProps<{ visible: boolean; productName: string; units: Product[] }>()
const emit = defineEmits<{
  (e: 'select', product: Product): void
  (e: 'cancel'): void
}>()

function handleSelect(unit: Product) {
  if (productUnavailableReason(unit)) return
  emit('select', unit)
}
</script>

<template>
  <Transition name="modal">
    <div v-if="visible" class="modal-backdrop" @click.self="emit('cancel')">
      <div class="modal-card">
        <div class="modal-header">
          <span class="modal-icon">📦</span>
          <h2 class="modal-title">{{ productName }}</h2>
        </div>

        <div class="modal-body">
          <p class="modal-hint">Elegí la presentación a vender</p>
          <div class="unit-list">
            <button
              v-for="unit in units"
              :key="unit.id"
              class="unit-chip"
              :class="{ 'unit-chip--unavailable': productUnavailableReason(unit) }"
              type="button"
              :disabled="!!productUnavailableReason(unit)"
              @click="handleSelect(unit)"
            >
              <span class="unit-name">{{ unit.unitName }}</span>
              <span class="unit-price">{{ formatCurrency(unit.unitPrice) }}</span>
              <span v-if="productUnavailableReason(unit)" class="unit-reason">{{ productUnavailableReason(unit) }}</span>
              <span v-else class="unit-stock">Stock: {{ unit.stockQuantity }}</span>
            </button>
          </div>
        </div>

        <div class="modal-footer">
          <button class="btn-cancel" @click="emit('cancel')">Cancelar</button>
        </div>
      </div>
    </div>
  </Transition>
</template>

<style scoped>
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
  width: 380px;
  max-width: calc(100vw - 32px);
  box-shadow: 0 24px 64px rgba(0, 0, 0, 0.6), 0 0 0 1px rgba(242, 141, 53, 0.08);
  overflow: hidden;
}

.modal-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 18px 20px 14px;
  border-bottom: 1px solid var(--color-border);
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
  flex-direction: column;
  gap: 12px;
}

.modal-hint {
  font-size: 11.5px;
  color: var(--color-text-muted);
}

.unit-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.unit-chip {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  background: var(--color-surface-3);
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all 0.15s;
  text-align: left;
}

.unit-chip:hover {
  border-color: var(--color-primary);
  background: rgba(242, 141, 53, 0.08);
}

.unit-chip--unavailable {
  opacity: 0.5;
  cursor: not-allowed;
}

.unit-chip--unavailable:hover {
  border-color: var(--color-border);
  background: var(--color-surface-3);
}

.unit-name {
  font-size: 13px;
  font-weight: 700;
  color: var(--color-cream);
  flex: 1;
}

.unit-price {
  font-size: 14px;
  font-weight: 800;
  color: var(--color-primary);
}

.unit-stock {
  font-size: 10.5px;
  font-weight: 600;
  color: var(--color-text-muted);
  min-width: 72px;
  text-align: right;
}

.unit-reason {
  font-size: 10.5px;
  font-weight: 700;
  color: var(--color-danger);
  text-transform: uppercase;
  min-width: 72px;
  text-align: right;
}

.modal-footer {
  display: flex;
  gap: 8px;
  padding: 14px 20px;
  border-top: 1px solid var(--color-border);
  justify-content: flex-end;
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

.btn-cancel:hover {
  border-color: var(--color-text-muted);
  color: var(--color-cream);
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

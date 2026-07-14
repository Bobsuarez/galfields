<script setup lang="ts">
import type { PaymentMethodSales } from '../composables/useReports'

const props = defineProps<{ methods: PaymentMethodSales[] }>()

const maxTotal = () => Math.max(...props.methods.map(m => m.total), 1)

function fmt(v: number): string {
  return '$' + v.toLocaleString('es-CO')
}
</script>

<template>
  <div class="methods-card">
    <div class="card-header">
      <h3 class="card-title">Ventas por Método de Pago</h3>
      <span class="card-badge">{{ methods.length }}</span>
    </div>
    <div v-if="methods.length === 0" class="empty-state">Sin ventas en este periodo</div>
    <div v-else class="methods-list">
      <div v-for="(m, i) in methods" :key="m.method" class="method-row">
        <span class="method-rank">{{ i + 1 }}</span>
        <div class="method-info">
          <div class="method-top">
            <span class="method-name">{{ m.method }}</span>
            <span class="method-total">{{ fmt(m.total) }}</span>
          </div>
          <div class="method-bar-track">
            <div class="method-bar-fill" :style="{ width: (m.total / maxTotal() * 100) + '%' }" />
          </div>
          <span class="method-count">{{ m.saleCount }} venta{{ m.saleCount === 1 ? '' : 's' }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.methods-card {
  background: var(--color-surface-2);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  height: 100%;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-title {
  font-size: 13px;
  font-weight: 700;
  color: var(--color-cream);
}

.card-badge {
  font-size: 10px;
  font-weight: 700;
  background: rgba(242, 141, 53, 0.15);
  color: var(--color-primary);
  padding: 2px 8px;
  border-radius: 10px;
}

.empty-state {
  font-size: 12px;
  color: var(--color-text-muted);
  padding: 8px 0;
}

.methods-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.method-row {
  display: flex;
  align-items: flex-start;
  gap: 8px;
}

.method-rank {
  font-size: 10px;
  font-weight: 700;
  color: var(--color-text-muted);
  width: 14px;
  text-align: right;
  flex-shrink: 0;
  margin-top: 1px;
}

.method-info {
  flex: 1;
  min-width: 0;
}

.method-top {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: 4px;
}

.method-name {
  font-size: 12px;
  color: var(--color-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.method-total {
  font-size: 12px;
  font-weight: 700;
  color: var(--color-cream);
  flex-shrink: 0;
  margin-left: 6px;
}

.method-bar-track {
  height: 4px;
  background: rgba(242, 227, 153, 0.06);
  border-radius: 2px;
  overflow: hidden;
  margin-bottom: 3px;
}

.method-bar-fill {
  height: 100%;
  background: var(--color-primary);
  border-radius: 2px;
  transition: width 0.6s ease;
}

.method-count {
  font-size: 10px;
  color: var(--color-text-muted);
}
</style>

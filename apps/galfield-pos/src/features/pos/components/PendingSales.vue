<script setup lang="ts">
import type { PendingSale } from '../../../types'
import { formatCurrency } from '../../../utils/currency'
import { getSaleEmoji } from '../composables/useActiveSale'

defineProps<{ sales: PendingSale[] }>()
const emit = defineEmits<{ (e: 'resume', sale: PendingSale): void }>()

function formatTime(date: Date): string {
  return new Date(date).toLocaleTimeString('es-CO', { hour: '2-digit', minute: '2-digit' })
}
</script>

<template>
  <div class="pending-sales">
    <div class="pending-header">
      <span class="pending-icon">🔄</span>
      <span class="pending-title">En Espera</span>
      <span class="pending-count" :class="{ 'pending-count--pulse': sales.length > 0 }">
        {{ sales.length }}
      </span>
    </div>

    <div class="pending-list">
      <div v-if="sales.length === 0" class="pending-empty">
        Sin ventas guardadas
      </div>

      <button
        v-for="sale in sales"
        :key="sale.id"
        class="pending-card"
        @click="emit('resume', sale)"
      >
        <div class="card-avatar">{{ getSaleEmoji(sale.iconKey) }}</div>
        <div class="card-info">
          <span class="card-label">{{ sale.label }}</span>
          <span class="card-time">{{ formatTime(sale.createdAt) }}</span>
        </div>
        <span class="card-total">{{ formatCurrency(sale.total) }}</span>
      </button>
    </div>
  </div>
</template>

<style scoped>
.pending-sales {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 16px;
  height: 68px;
  background: var(--color-surface);
  border-top: 1px solid var(--color-border);
  flex-shrink: 0;
}

.pending-header {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
  min-width: 100px;
}

.pending-icon {
  font-size: 16px;
  line-height: 1;
}

.pending-title {
  font-size: 10px;
  font-weight: 700;
  color: var(--color-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.8px;
  white-space: nowrap;
}

.pending-count {
  background: var(--color-primary);
  color: #0D0D0D;
  font-size: 10px;
  font-weight: 800;
  min-width: 20px;
  height: 20px;
  border-radius: 10px;
  padding: 0 5px;
  display: flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
}

.pending-count--pulse {
  animation: badge-pulse 2.5s ease-in-out infinite;
}

@keyframes badge-pulse {
  0%, 100% { box-shadow: 0 0 0 0 rgba(242, 141, 53, 0.4); }
  50%       { box-shadow: 0 0 0 5px rgba(242, 141, 53, 0);  }
}

.pending-list {
  display: flex;
  align-items: center;
  gap: 8px;
  overflow-x: auto;
  flex: 1;
  height: 100%;
  padding: 10px 0;
}

.pending-list::-webkit-scrollbar { height: 2px; }

.pending-empty {
  font-size: 11px;
  color: var(--color-text-dim);
  white-space: nowrap;
  padding: 0 4px;
}

.pending-card {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 14px 0 10px;
  height: 46px;
  background: var(--color-surface-2);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: border-color 0.15s, box-shadow 0.15s, background 0.15s;
  white-space: nowrap;
  flex-shrink: 0;
  text-align: left;
}

.pending-card:hover {
  border-color: var(--color-primary);
  background: rgba(242, 141, 53, 0.06);
  box-shadow: 0 0 0 2px rgba(242, 141, 53, 0.12), 0 4px 12px rgba(0, 0, 0, 0.2);
}

.card-avatar {
  font-size: 20px;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(242, 141, 53, 0.1);
  border-radius: 8px;
  flex-shrink: 0;
  line-height: 1;
}

.card-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.card-label {
  font-size: 12px;
  font-weight: 600;
  color: var(--color-cream);
  max-width: 90px;
  overflow: hidden;
  text-overflow: ellipsis;
}

.card-time {
  font-size: 9.5px;
  color: var(--color-text-dim);
  font-variant-numeric: tabular-nums;
}

.card-total {
  font-size: 13px;
  font-weight: 700;
  color: var(--color-primary);
  font-variant-numeric: tabular-nums;
}
</style>

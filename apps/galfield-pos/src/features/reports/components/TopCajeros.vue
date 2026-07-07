<script setup lang="ts">
import type { TopCajero } from '../composables/useReports'

const props = defineProps<{ cajeros: TopCajero[] }>()

const maxSales = () => Math.max(...props.cajeros.map(c => c.sales), 1)

const AVATARS = ['👩', '👨', '👩', '👨', '👩', '👨', '👩']

function fmt(v: number): string {
  return '$' + v.toLocaleString('es-CO')
}
</script>

<template>
  <div class="cajeros-card">
    <div class="card-header">
      <h3 class="card-title">Top Cajeros por Ventas</h3>
      <span class="card-badge">Top 7</span>
    </div>
    <div class="cajeros-list">
      <div v-for="(c, i) in cajeros" :key="c.name" class="cajero-row">
        <span class="cajero-rank">{{ i + 1 }}</span>
        <span class="cajero-avatar">{{ AVATARS[i] }}</span>
        <div class="cajero-info">
          <div class="cajero-top">
            <span class="cajero-name">{{ c.name }}</span>
            <span class="cajero-sales">{{ fmt(c.sales) }}</span>
          </div>
          <div class="cajero-bar-track">
            <div
              class="cajero-bar-fill"
              :style="{ width: (c.sales / maxSales() * 100) + '%' }"
            />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.cajeros-card {
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

.cajeros-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.cajero-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.cajero-rank {
  font-size: 10px;
  font-weight: 700;
  color: var(--color-text-muted);
  width: 14px;
  text-align: right;
  flex-shrink: 0;
}

.cajero-avatar {
  font-size: 20px;
  line-height: 1;
  flex-shrink: 0;
}

.cajero-info {
  flex: 1;
  min-width: 0;
}

.cajero-top {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: 4px;
}

.cajero-name {
  font-size: 12px;
  color: var(--color-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.cajero-sales {
  font-size: 12px;
  font-weight: 700;
  color: var(--color-cream);
  flex-shrink: 0;
  margin-left: 6px;
}

.cajero-bar-track {
  height: 4px;
  background: rgba(242, 227, 153, 0.06);
  border-radius: 2px;
  overflow: hidden;
}

.cajero-bar-fill {
  height: 100%;
  background: var(--color-primary);
  border-radius: 2px;
  transition: width 0.6s ease;
}
</style>

<script setup lang="ts">
import type { KpiMetric } from '../composables/useReports'

const props = defineProps<{ metric: KpiMetric }>()

function formatValue(v: number): string {
  if (props.metric.prefix === '$') {
    return '$' + v.toLocaleString('es-CO')
  }
  return v.toLocaleString('es-CO')
}
</script>

<template>
  <div class="kpi-card">
    <p class="kpi-label">{{ metric.label }}</p>
    <p class="kpi-value">{{ formatValue(metric.value) }}</p>
    <div class="kpi-change" :class="metric.change >= 0 ? 'change--up' : 'change--down'">
      <span class="change-arrow">{{ metric.change >= 0 ? '▲' : '▼' }}</span>
      <span>{{ Math.abs(metric.change) }}%</span>
      <span class="change-label">vs anterior</span>
    </div>
  </div>
</template>

<style scoped>
.kpi-card {
  background: var(--color-surface-2);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 16px 18px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  transition: border-color 0.2s;
}

.kpi-card:hover {
  border-color: rgba(242, 141, 53, 0.3);
}

.kpi-label {
  font-size: 11.5px;
  color: var(--color-text-muted);
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.4px;
}

.kpi-value {
  font-size: 22px;
  font-weight: 800;
  color: var(--color-cream);
  line-height: 1;
}

.kpi-change {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  font-weight: 600;
}

.change--up { color: #4CAF50; }
.change--down { color: #EF5350; }

.change-arrow { font-size: 10px; }

.change-label {
  font-size: 11px;
  font-weight: 400;
  color: var(--color-text-muted);
  margin-left: 2px;
}
</style>

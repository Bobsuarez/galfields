<script setup lang="ts">
import type { LowStockItem } from '../composables/useReports'

defineProps<{ items: LowStockItem[] }>()
</script>

<template>
  <div class="lowstock-card">
    <div class="card-header">
      <h3 class="card-title">Productos con Bajo Stock</h3>
      <span class="badge--warn">⚠ Atención</span>
    </div>
    <table class="lowstock-table">
      <thead>
        <tr>
          <th>Producto</th>
          <th>Actual</th>
          <th>Mín.</th>
          <th>Ventas</th>
          <th>Estado</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="item in items" :key="item.name">
          <td class="item-name">{{ item.name }}</td>
          <td class="stock-val">{{ item.currentStock }}</td>
          <td class="stock-min">{{ item.minStock }}</td>
          <td class="sales-val">{{ item.sales }}</td>
          <td>
            <span class="status-chip" :class="`chip--${item.status}`">
              {{ item.status === 'critico' ? 'Crítico' : 'Bajo' }}
            </span>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<style scoped>
.lowstock-card {
  background: var(--color-surface-2);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 10px;
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

.badge--warn {
  font-size: 10px;
  font-weight: 700;
  background: rgba(255, 152, 0, 0.15);
  color: #FFB74D;
  padding: 2px 8px;
  border-radius: 10px;
}

.lowstock-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 12px;
}

.lowstock-table th {
  text-align: left;
  color: var(--color-text-muted);
  font-size: 10px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.4px;
  padding: 6px 8px 8px;
  border-bottom: 1px solid var(--color-border);
}

.lowstock-table td {
  padding: 8px 8px;
  border-bottom: 1px solid rgba(242, 227, 153, 0.04);
  color: var(--color-text);
}

.lowstock-table tr:last-child td { border-bottom: none; }

.item-name { color: var(--color-text); font-weight: 500; }

.stock-val {
  font-weight: 700;
  color: #EF5350;
}

.stock-min { color: var(--color-text-muted); }

.sales-val { color: var(--color-text-muted); }

.status-chip {
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 10.5px;
  font-weight: 700;
}

.chip--critico {
  background: rgba(229, 57, 53, 0.15);
  color: #EF5350;
}

.chip--bajo {
  background: rgba(255, 152, 0, 0.15);
  color: #FFB74D;
}
</style>

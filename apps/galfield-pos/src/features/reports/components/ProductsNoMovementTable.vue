<script setup lang="ts">
import type { ProductNoMovement } from '../composables/useReports'

defineProps<{ items: ProductNoMovement[] }>()
</script>

<template>
  <div class="nomovement-card">
    <div class="card-header">
      <h3 class="card-title">Sin Movimiento</h3>
      <span class="badge--warn">⏸ Stock muerto</span>
    </div>
    <p v-if="items.length === 0" class="empty-state">Todos los productos activos tuvieron ventas en este periodo</p>
    <table v-else class="nomovement-table">
      <thead>
        <tr>
          <th>Producto</th>
          <th>Categoría</th>
          <th>Stock</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="item in items" :key="item.name">
          <td class="item-name">{{ item.name }}</td>
          <td class="item-category">{{ item.category }}</td>
          <td class="stock-val">{{ item.stock }}</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<style scoped>
.nomovement-card {
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

.empty-state {
  font-size: 12px;
  color: var(--color-text-muted);
  padding: 4px 0;
}

.nomovement-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 12px;
}

.nomovement-table th {
  text-align: left;
  color: var(--color-text-muted);
  font-size: 10px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.4px;
  padding: 6px 8px 8px;
  border-bottom: 1px solid var(--color-border);
}

.nomovement-table td {
  padding: 8px 8px;
  border-bottom: 1px solid rgba(242, 227, 153, 0.04);
  color: var(--color-text);
}

.nomovement-table tr:last-child td { border-bottom: none; }

.item-name { color: var(--color-text); font-weight: 500; }

.item-category { color: var(--color-text-muted); }

.stock-val { color: var(--color-text-muted); }
</style>

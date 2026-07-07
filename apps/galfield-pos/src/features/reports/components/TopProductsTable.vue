<script setup lang="ts">
import type { TopProduct } from '../composables/useReports'

defineProps<{ products: TopProduct[] }>()

function fmt(v: number): string {
  return '$' + v.toLocaleString('es-CO')
}
</script>

<template>
  <div class="products-card">
    <div class="card-header">
      <h3 class="card-title">Top Productos Vendidos</h3>
      <span class="card-badge">Top 20</span>
    </div>
    <table class="products-table">
      <thead>
        <tr>
          <th>#</th>
          <th>Producto</th>
          <th>Ingresos</th>
          <th>Cant.</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="p in products" :key="p.rank">
          <td>
            <span class="rank" :class="p.rank <= 3 ? `rank--${p.rank}` : ''">{{ p.rank }}</span>
          </td>
          <td class="product-name">{{ p.name }}</td>
          <td class="revenue">{{ fmt(p.revenue) }}</td>
          <td class="qty">{{ p.quantity }}</td>
        </tr>
      </tbody>
    </table>
    <button class="see-more">Consultar más detalles →</button>
  </div>
</template>

<style scoped>
.products-card {
  background: var(--color-surface-2);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 10px;
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

.products-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 12px;
}

.products-table th {
  text-align: left;
  color: var(--color-text-muted);
  font-size: 10px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.4px;
  padding: 6px 6px 8px;
  border-bottom: 1px solid var(--color-border);
}

.products-table td {
  padding: 7px 6px;
  border-bottom: 1px solid rgba(242, 227, 153, 0.04);
  color: var(--color-text);
}

.products-table tr:last-child td { border-bottom: none; }

.rank {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  font-size: 10px;
  font-weight: 700;
  background: rgba(242, 227, 153, 0.08);
  color: var(--color-text-muted);
}

.rank--1 { background: rgba(255, 215, 0, 0.2); color: #FFD700; }
.rank--2 { background: rgba(192, 192, 192, 0.2); color: #C0C0C0; }
.rank--3 { background: rgba(205, 127, 50, 0.2); color: #CD7F32; }

.product-name { color: var(--color-text); }

.revenue {
  font-weight: 700;
  color: var(--color-cream);
}

.qty {
  color: var(--color-text-muted);
  text-align: right;
}

.see-more {
  background: transparent;
  border: none;
  color: var(--color-primary);
  font-size: 11.5px;
  font-weight: 600;
  cursor: pointer;
  padding: 4px 0;
  text-align: left;
  transition: opacity 0.15s;
}

.see-more:hover { opacity: 0.75; }
</style>

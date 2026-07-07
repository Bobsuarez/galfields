<script setup lang="ts">
import type { FinancialData } from '../composables/useReports'

defineProps<{ data: FinancialData }>()

function fmt(v: number): string {
  return '$' + v.toLocaleString('es-CO')
}
</script>

<template>
  <div class="financial-card">
    <h3 class="card-title">Resumen Financiero</h3>

    <div class="summary-boxes">
      <div class="summary-box summary-box--gross">
        <p class="box-label">Ventas Brutas</p>
        <p class="box-value">{{ fmt(data.grossSales) }}</p>
      </div>
      <div class="summary-box summary-box--discount">
        <p class="box-label">Descuentos</p>
        <p class="box-value box-value--neg">-{{ fmt(data.discounts) }}</p>
      </div>
      <div class="summary-box summary-box--net">
        <p class="box-label">Ventas Netas</p>
        <p class="box-value box-value--accent">{{ fmt(data.netSales) }}</p>
      </div>
    </div>

    <div v-if="data.grossMargin !== undefined && data.marginPercent !== undefined && data.cogs !== undefined" class="margin-row">
      <div class="margin-item">
        <p class="margin-label">Margen Bruto</p>
        <p class="margin-value">{{ fmt(data.grossMargin) }}</p>
      </div>
      <div class="margin-divider" />
      <div class="margin-item margin-item--center">
        <p class="margin-label">Margen %</p>
        <p class="margin-pct">{{ data.marginPercent }}%</p>
      </div>
      <div class="margin-divider" />
      <div class="margin-item">
        <p class="margin-label">Costo de Ventas</p>
        <p class="margin-value margin-value--muted">{{ fmt(data.cogs) }}</p>
      </div>
    </div>
    <div v-else class="margin-row margin-row--placeholder">
      <p class="margin-placeholder-text">
        📊 Margen y costo de ventas próximamente — requiere registrar el costo de compra por producto.
      </p>
    </div>

    <div class="tip-box">
      <span class="tip-emoji">🐱</span>
      <div class="tip-content">
        <p class="tip-title">¿Sabías que?</p>
        <p class="tip-text">Puedes usar teclas rápidas para agilizar tu trabajo. Presiona F1–F5 para ver los atajos disponibles.</p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.financial-card {
  background: var(--color-surface-2);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.card-title {
  font-size: 13px;
  font-weight: 700;
  color: var(--color-cream);
}

.summary-boxes {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 8px;
}

.summary-box {
  border-radius: var(--radius-sm);
  padding: 10px 12px;
  border: 1px solid transparent;
}

.summary-box--gross {
  background: rgba(242, 227, 153, 0.05);
  border-color: rgba(242, 227, 153, 0.1);
}

.summary-box--discount {
  background: rgba(229, 57, 53, 0.06);
  border-color: rgba(229, 57, 53, 0.12);
}

.summary-box--net {
  background: rgba(242, 141, 53, 0.1);
  border-color: rgba(242, 141, 53, 0.2);
}

.box-label {
  font-size: 10px;
  color: var(--color-text-muted);
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.4px;
  margin-bottom: 4px;
}

.box-value {
  font-size: 14px;
  font-weight: 800;
  color: var(--color-cream);
  white-space: nowrap;
}

.box-value--neg { color: #EF5350; }
.box-value--accent { color: var(--color-primary); }

.margin-row {
  display: flex;
  align-items: center;
  background: rgba(242, 141, 53, 0.06);
  border: 1px solid rgba(242, 141, 53, 0.12);
  border-radius: var(--radius-sm);
  padding: 10px 14px;
  gap: 0;
}

.margin-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.margin-item--center { align-items: center; }

.margin-divider {
  width: 1px;
  height: 32px;
  background: rgba(242, 141, 53, 0.2);
  margin: 0 14px;
}

.margin-label {
  font-size: 10px;
  color: var(--color-text-muted);
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.3px;
}

.margin-value {
  font-size: 15px;
  font-weight: 800;
  color: #4CAF50;
}

.margin-value--muted { color: var(--color-text-muted); font-size: 13px; font-weight: 600; }

.margin-pct {
  font-size: 22px;
  font-weight: 900;
  color: var(--color-primary);
}

.margin-row--placeholder {
  justify-content: center;
}

.margin-placeholder-text {
  font-size: 11.5px;
  color: var(--color-text-muted);
  text-align: center;
  line-height: 1.5;
}

.tip-box {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  background: rgba(140, 80, 27, 0.12);
  border: 1px solid rgba(140, 80, 27, 0.25);
  border-radius: var(--radius-sm);
  padding: 10px 12px;
}

.tip-emoji { font-size: 28px; line-height: 1; flex-shrink: 0; }

.tip-content { flex: 1; }

.tip-title {
  font-size: 11.5px;
  font-weight: 700;
  color: var(--color-primary);
  margin-bottom: 3px;
}

.tip-text {
  font-size: 11px;
  color: var(--color-text-muted);
  line-height: 1.5;
}
</style>

<script setup lang="ts">
import { useReports } from './composables/useReports'
import KpiCard from './components/KpiCard.vue'
import ReportsPasswordModal from './components/ReportsPasswordModal.vue'
import TopProductsTable from './components/TopProductsTable.vue'
import TopCajeros from './components/TopCajeros.vue'
import LowStockTable from './components/LowStockTable.vue'
import FinancialSummary from './components/FinancialSummary.vue'
import LineChart from '../../components/shared/charts/LineChart.vue'
import DonutChart from '../../components/shared/charts/DonutChart.vue'
import BarChart from '../../components/shared/charts/BarChart.vue'

const {
  dateFrom,
  dateTo,
  kpis,
  isLoadingKpis,
  showPasswordGate,
  applyDateRange,
  confirmPasswordGate,
  cancelPasswordGate,
  lineLabels,
  lineSeries,
  categorySales,
  barData,
  topProducts,
  topCajeros,
  lowStock,
  financial,
} = useReports()

function fmtK(v: number): string {
  if (v >= 1000) return '$' + Math.round(v / 1000) + 'k'
  return '$' + v
}

function fmtBar(v: number): string {
  if (v >= 1000) return Math.round(v / 1000) + 'k'
  return String(v)
}
</script>

<template>
  <main class="reports-view">
    <!-- Header -->
    <div class="reports-header">
      <div class="header-left">
        <h1 class="reports-title">Reportes</h1>
        <p class="reports-subtitle">Analiza el rendimiento de tu negocio</p>
      </div>
      <div class="header-right">
        <div class="date-range">
          <input v-model="dateFrom" type="date" class="date-input" />
          <span class="date-sep">—</span>
          <input v-model="dateTo" type="date" class="date-input" />
        </div>
        <button class="btn-filter" @click="applyDateRange">
          <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <polygon points="22 3 2 3 10 12.46 10 19 14 21 14 12.46 22 3"/>
          </svg>
          Filtros
        </button>
        <button class="btn-export">
          <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/>
          </svg>
          Exportar
        </button>
      </div>
    </div>

    <div class="reports-scroll">
      <!-- KPI Row -->
      <div class="kpi-grid" :class="{ 'kpi-grid--loading': isLoadingKpis }">
        <KpiCard v-for="m in kpis" :key="m.label" :metric="m" />
      </div>

      <!-- Charts Row -->
      <div class="charts-row">
        <div class="chart-card chart-card--line">
          <div class="chart-header">
            <div>
              <h3 class="chart-title">Ventas por Día</h3>
              <p class="chart-subtitle">{{ dateFrom }} → {{ dateTo }}</p>
            </div>
            <div class="chart-legend">
              <span class="legend-item">
                <span class="legend-dot" style="background:#F28D35" />
                Esta semana
              </span>
              <span class="legend-item">
                <span class="legend-dot legend-dot--dashed" />
                Semana anterior
              </span>
            </div>
          </div>
          <div class="chart-body">
            <LineChart :labels="lineLabels" :series="lineSeries" :format-y="fmtK" />
          </div>
        </div>

        <div class="chart-card chart-card--donut">
          <div class="chart-header">
            <h3 class="chart-title">Categorías</h3>
          </div>
          <div class="chart-body chart-body--donut">
            <DonutChart :segments="categorySales" center-label="100%" />
          </div>
        </div>
      </div>

      <!-- Middle Row: Top Products + Bar Chart + Cajeros -->
      <div class="middle-row">
        <TopProductsTable :products="topProducts" />

        <div class="chart-card">
          <div class="chart-header">
            <h3 class="chart-title">Ventas por Hora</h3>
          </div>
          <div class="chart-body">
            <BarChart :data="barData" color="#F28D35" :format-value="fmtBar" />
          </div>
        </div>

        <TopCajeros :cajeros="topCajeros" />
      </div>

      <!-- Bottom Row: Low Stock + Financial -->
      <div class="bottom-row">
        <LowStockTable :items="lowStock" />
        <FinancialSummary :data="financial" />
      </div>
    </div>

    <ReportsPasswordModal
      :visible="showPasswordGate"
      @confirm="confirmPasswordGate"
      @cancel="cancelPasswordGate"
    />
  </main>
</template>

<style scoped>
.reports-view {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: var(--color-bg);
  overflow: hidden;
}

.reports-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 20px;
  border-bottom: 1px solid var(--color-border);
  flex-shrink: 0;
  background: var(--color-surface);
}

.header-left {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.reports-title {
  font-size: 18px;
  font-weight: 800;
  color: var(--color-cream);
}

.reports-subtitle {
  font-size: 11.5px;
  color: var(--color-text-muted);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.date-range {
  display: flex;
  align-items: center;
  gap: 6px;
  background: var(--color-surface-2);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  padding: 4px 10px;
}

.date-input {
  background: transparent;
  border: none;
  color: var(--color-text);
  font-size: 12px;
  font-family: inherit;
  cursor: pointer;
}

.date-input::-webkit-calendar-picker-indicator {
  filter: invert(0.6);
  cursor: pointer;
}

.date-sep {
  color: var(--color-text-muted);
  font-size: 12px;
}

.btn-filter {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 7px 12px;
  background: var(--color-surface-2);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  color: var(--color-text-muted);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
  font-family: inherit;
}

.btn-filter:hover {
  border-color: rgba(242, 141, 53, 0.3);
  color: var(--color-cream);
}

.btn-export {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 7px 14px;
  background: var(--color-primary);
  border: none;
  border-radius: var(--radius-sm);
  color: #0D0D0D;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  transition: background 0.15s;
  font-family: inherit;
}

.btn-export:hover { background: var(--color-primary-hover); }

.reports-scroll {
  flex: 1;
  overflow-y: auto;
  padding: 16px 20px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  transition: opacity 0.15s;
}

.kpi-grid--loading {
  opacity: 0.5;
  pointer-events: none;
}

.charts-row {
  display: grid;
  grid-template-columns: 1fr 360px;
  gap: 12px;
}

.chart-card {
  background: var(--color-surface-2);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.chart-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}

.chart-title {
  font-size: 13px;
  font-weight: 700;
  color: var(--color-cream);
}

.chart-subtitle {
  font-size: 10.5px;
  color: var(--color-text-muted);
  margin-top: 2px;
}

.chart-legend {
  display: flex;
  gap: 12px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 11px;
  color: var(--color-text-muted);
}

.legend-dot {
  width: 20px;
  height: 3px;
  border-radius: 2px;
  flex-shrink: 0;
  background: #F28D35;
}

.legend-dot--dashed {
  background: repeating-linear-gradient(
    to right,
    #8C501B 0px,
    #8C501B 4px,
    transparent 4px,
    transparent 7px
  );
}

.chart-body {
  flex: 1;
}

.chart-body--donut {
  display: flex;
  align-items: center;
}

.middle-row {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 12px;
  align-items: start;
}

.bottom-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}
</style>

<script setup lang="ts">
import { computed } from 'vue'

interface Segment {
  label: string
  value: number
  color: string
}

const props = defineProps<{ segments: Segment[]; centerLabel?: string }>()

const CX = 90, CY = 90, R = 68, IR = 44
const total = computed(() => props.segments.reduce((s, x) => s + x.value, 0))

interface ArcDef { d: string; color: string; label: string; value: number; pct: string }

const arcs = computed((): ArcDef[] => {
  let cumAngle = 0
  return props.segments.map(seg => {
    const angle = (seg.value / (total.value || 1)) * 360
    const startAngle = cumAngle
    const endAngle = cumAngle + angle
    cumAngle += angle
    const arc = describeArc(CX, CY, R, IR, startAngle, endAngle - 0.3)
    return {
      d: arc,
      color: seg.color,
      label: seg.label,
      value: seg.value,
      pct: ((seg.value / (total.value || 1)) * 100).toFixed(1) + '%',
    }
  })
})

function describeArc(cx: number, cy: number, r: number, ir: number, start: number, end: number): string {
  const toR = (deg: number) => (deg - 90) * Math.PI / 180
  const sr = toR(start), er = toR(end)
  const x1 = cx + r * Math.cos(sr), y1 = cy + r * Math.sin(sr)
  const x2 = cx + r * Math.cos(er), y2 = cy + r * Math.sin(er)
  const ix1 = cx + ir * Math.cos(sr), iy1 = cy + ir * Math.sin(sr)
  const ix2 = cx + ir * Math.cos(er), iy2 = cy + ir * Math.sin(er)
  const large = end - start > 180 ? 1 : 0
  return `M${x1},${y1} A${r},${r} 0 ${large},1 ${x2},${y2} L${ix2},${iy2} A${ir},${ir} 0 ${large},0 ${ix1},${iy1} Z`
}
</script>

<template>
  <div class="donut-wrap">
    <svg viewBox="0 0 180 180" width="180" height="180" style="flex-shrink:0">
      <path
        v-for="(arc, i) in arcs"
        :key="i"
        :d="arc.d"
        :fill="arc.color"
        stroke="#111"
        stroke-width="1.5"
      />
      <text x="90" y="86" text-anchor="middle" font-size="11" fill="rgba(242,227,153,0.55)" font-weight="500">Total</text>
      <text x="90" y="100" text-anchor="middle" font-size="14" fill="#F2E399" font-weight="700">
        {{ centerLabel ?? total + '%' }}
      </text>
    </svg>
    <div class="donut-legend">
      <div v-for="arc in arcs" :key="arc.label" class="legend-row">
        <span class="legend-dot" :style="{ background: arc.color }" />
        <span class="legend-label">{{ arc.label }}</span>
        <span class="legend-pct">{{ arc.pct }}</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.donut-wrap {
  display: flex;
  align-items: center;
  gap: 16px;
  width: 100%;
}

.donut-legend {
  display: flex;
  flex-direction: column;
  gap: 6px;
  flex: 1;
}

.legend-row {
  display: flex;
  align-items: center;
  gap: 7px;
}

.legend-dot {
  width: 10px;
  height: 10px;
  border-radius: 2px;
  flex-shrink: 0;
}

.legend-label {
  font-size: 12px;
  color: var(--color-cream);
  flex: 1;
}

.legend-pct {
  font-size: 12px;
  font-weight: 700;
  color: var(--color-primary);
  min-width: 36px;
  text-align: right;
}
</style>

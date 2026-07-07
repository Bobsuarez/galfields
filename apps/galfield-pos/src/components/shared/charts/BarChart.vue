<script setup lang="ts">
import { computed } from 'vue'

interface BarData {
  label: string
  value: number
}

const props = withDefaults(defineProps<{
  data: BarData[]
  color?: string
  formatValue?: (v: number) => string
}>(), {
  color: '#F28D35',
  formatValue: (v: number) => String(v),
})

const W = 400
const H = 160
const PAD = { top: 18, right: 10, bottom: 30, left: 10 }
const plotW = W - PAD.left - PAD.right
const plotH = H - PAD.top - PAD.bottom

const maxValue = computed(() => Math.max(...props.data.map(d => d.value), 1))

const barWidth = computed(() => {
  const n = props.data.length
  return n > 0 ? (plotW / n) * 0.65 : 0
})

function barX(i: number): number {
  const n = props.data.length
  const slotW = plotW / n
  return PAD.left + i * slotW + (slotW - barWidth.value) / 2
}

function barH(v: number): number {
  return plotH * (v / maxValue.value)
}

function barY(v: number): number {
  return PAD.top + plotH - barH(v)
}
</script>

<template>
  <svg :viewBox="`0 0 ${W} ${H}`" width="100%" style="display:block; overflow:visible">
    <!-- Grid lines -->
    <line
      v-for="i in 4"
      :key="i"
      :x1="PAD.left" :y1="PAD.top + plotH * (1 - i / 4)"
      :x2="W - PAD.right" :y2="PAD.top + plotH * (1 - i / 4)"
      stroke="rgba(242,227,153,0.06)" stroke-width="1"
    />

    <!-- Bars -->
    <g v-for="(d, i) in data" :key="i">
      <rect
        :x="barX(i)"
        :y="barY(d.value)"
        :width="barWidth"
        :height="barH(d.value)"
        :fill="color"
        rx="3"
        opacity="0.85"
      />
      <!-- Value on top -->
      <text
        v-if="barH(d.value) > 12"
        :x="barX(i) + barWidth / 2"
        :y="barY(d.value) - 4"
        text-anchor="middle"
        font-size="9"
        fill="rgba(242,227,153,0.7)"
      >{{ formatValue(d.value) }}</text>
      <!-- X label -->
      <text
        :x="barX(i) + barWidth / 2"
        :y="H - PAD.bottom + 14"
        text-anchor="middle"
        font-size="10"
        fill="rgba(242,227,153,0.5)"
      >{{ d.label }}</text>
    </g>

    <!-- Base line -->
    <line
      :x1="PAD.left" :y1="H - PAD.bottom"
      :x2="W - PAD.right" :y2="H - PAD.bottom"
      stroke="rgba(242,141,53,0.2)" stroke-width="1"
    />
  </svg>
</template>

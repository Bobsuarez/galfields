<script setup lang="ts">
import { computed } from 'vue'

interface Series {
  name: string
  data: number[]
  color: string
  dashed?: boolean
}

const props = withDefaults(defineProps<{
  labels: string[]
  series: Series[]
  formatY?: (v: number) => string
}>(), {
  formatY: (v: number) => String(v),
})

const W = 500
const H = 200
const PAD = { top: 18, right: 16, bottom: 32, left: 52 }
const plotW = W - PAD.left - PAD.right
const plotH = H - PAD.top - PAD.bottom
const Y_TICKS = 5

const allValues = computed(() => props.series.flatMap(s => s.data))

const yScale = computed(() => {
  const rawMax = Math.max(...allValues.value, 0)
  const rawMin = Math.min(...allValues.value, 0)
  const range = rawMax - rawMin || 1
  const step = Math.pow(10, Math.floor(Math.log10(range / Y_TICKS)))
  const niceStep = Math.ceil((range / Y_TICKS) / step) * step || 1
  const yMin = 0
  const yMax = Math.ceil(rawMax / niceStep) * niceStep || niceStep
  const ticks = Array.from({ length: Y_TICKS + 1 }, (_, i) => yMin + i * (yMax - yMin) / Y_TICKS)
  return { min: yMin, max: yMax, ticks }
})

function yPos(v: number): number {
  const { min, max } = yScale.value
  return PAD.top + plotH * (1 - (v - min) / (max - min))
}

function xPos(i: number): number {
  return PAD.left + (props.labels.length > 1 ? i * plotW / (props.labels.length - 1) : plotW / 2)
}

function smoothLine(data: number[]): string {
  const pts = data.map((v, i) => ({ x: xPos(i), y: yPos(v) }))
  if (pts.length === 0) return ''
  if (pts.length === 1) return `M ${pts[0].x} ${pts[0].y}`
  let d = `M ${pts[0].x} ${pts[0].y}`
  for (let i = 1; i < pts.length; i++) {
    const cpx = (pts[i - 1].x + pts[i].x) / 2
    d += ` C ${cpx},${pts[i - 1].y} ${cpx},${pts[i].y} ${pts[i].x},${pts[i].y}`
  }
  return d
}

function areaPath(data: number[]): string {
  const line = smoothLine(data)
  if (!line) return ''
  const lastX = xPos(data.length - 1)
  const firstX = xPos(0)
  const baseY = yPos(yScale.value.min)
  return `${line} L ${lastX},${baseY} L ${firstX},${baseY} Z`
}
</script>

<template>
  <svg :viewBox="`0 0 ${W} ${H}`" width="100%" style="overflow: visible; display: block">
    <defs>
      <linearGradient v-for="s in series" :key="s.color + '-grad'" :id="`grad-${s.color.slice(1)}`" x1="0" y1="0" x2="0" y2="1">
        <stop offset="0%" :stop-color="s.color" stop-opacity="0.25" />
        <stop offset="100%" :stop-color="s.color" stop-opacity="0.02" />
      </linearGradient>
    </defs>

    <!-- Grid lines -->
    <line
      v-for="tick in yScale.ticks"
      :key="tick"
      :x1="PAD.left" :y1="yPos(tick)"
      :x2="W - PAD.right" :y2="yPos(tick)"
      stroke="rgba(242,227,153,0.07)" stroke-width="1"
    />

    <!-- Area fills -->
    <path
      v-for="s in series"
      :key="s.name + '-area'"
      :d="areaPath(s.data)"
      :fill="`url(#grad-${s.color.slice(1)})`"
    />

    <!-- Lines -->
    <path
      v-for="s in series"
      :key="s.name + '-line'"
      :d="smoothLine(s.data)"
      :stroke="s.color"
      :stroke-dasharray="s.dashed ? '5 3' : undefined"
      stroke-width="2"
      fill="none"
      stroke-linecap="round"
      stroke-linejoin="round"
    />

    <!-- Data points -->
    <template v-for="s in series" :key="s.name + '-dots'">
      <circle
        v-for="(v, i) in s.data"
        :key="i"
        :cx="xPos(i)" :cy="yPos(v)"
        r="3"
        :fill="s.color"
        stroke="#0D0D0D"
        stroke-width="1.5"
      />
    </template>

    <!-- Y axis labels -->
    <text
      v-for="tick in yScale.ticks"
      :key="tick + '-ylabel'"
      :x="PAD.left - 6" :y="yPos(tick) + 4"
      text-anchor="end"
      font-size="10"
      fill="rgba(242,227,153,0.45)"
    >{{ formatY(tick) }}</text>

    <!-- X axis labels -->
    <text
      v-for="(label, i) in labels"
      :key="label + i"
      :x="xPos(i)" :y="H - PAD.bottom + 14"
      text-anchor="middle"
      font-size="10.5"
      fill="rgba(242,227,153,0.55)"
    >{{ label }}</text>

    <!-- Axes -->
    <line :x1="PAD.left" :y1="PAD.top" :x2="PAD.left" :y2="H - PAD.bottom" stroke="rgba(242,141,53,0.2)" stroke-width="1" />
    <line :x1="PAD.left" :y1="H - PAD.bottom" :x2="W - PAD.right" :y2="H - PAD.bottom" stroke="rgba(242,141,53,0.2)" stroke-width="1" />
  </svg>
</template>

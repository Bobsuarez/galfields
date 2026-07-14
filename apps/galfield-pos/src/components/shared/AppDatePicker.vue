<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import AppIcon from './AppIcon.vue'

/**
 * Custom in-DOM date picker — not a wrapper around `<input type="date">`.
 * WebKitGTK (Tauri's Linux webview) doesn't reliably respond to clicks
 * inside the native date popup (the day never visibly selects, only
 * pressing Escape commits it), which is a bug in the OS-native widget
 * itself, not something a JS event listener can work around. Building the
 * calendar as plain DOM/Vue makes day selection a normal `@click` handler
 * we fully control, same reasoning as this app's hand-built SVG charts
 * instead of a charting library.
 *
 * Deliberately locked to the current calendar month, no prev/next
 * navigation — matches this app's own data-retention rule (the local DB
 * only keeps the current month's sales long-term, see useReports.ts).
 */
const props = defineProps<{ modelValue: string }>()
const emit = defineEmits<{ (e: 'update:modelValue', value: string): void }>()

const isOpen = ref(false)
const rootRef = ref<HTMLElement | null>(null)

const WEEKDAY_LABELS = ['Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb', 'Dom']
const MONTH_LABELS = [
  'Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio',
  'Julio', 'Agosto', 'Septiembre', 'Octubre', 'Noviembre', 'Diciembre',
]

const now   = new Date()
const year  = now.getFullYear()
const month = now.getMonth() // 0-indexed

const monthLabel = `${MONTH_LABELS[month]} ${year}`

const daysInMonth = new Date(year, month + 1, 0).getDate()

// JS getDay() is Sun-first (0-6); shift so the grid starts on Monday to
// match WEEKDAY_LABELS above.
const leadingBlanks = (new Date(year, month, 1).getDay() + 6) % 7

const cells = computed(() => [
  ...Array.from({ length: leadingBlanks }, () => null),
  ...Array.from({ length: daysInMonth }, (_, i) => i + 1),
])

function isoFor(day: number): string {
  return `${year}-${String(month + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`
}

const selectedDay = computed(() => {
  const [y, m, d] = props.modelValue.split('-').map(Number)
  return y === year && m === month + 1 ? d : null
})

const todayDay = now.getDate()

const displayLabel = computed(() => {
  if (!selectedDay.value) return 'Seleccionar fecha'
  return `${String(selectedDay.value).padStart(2, '0')}/${String(month + 1).padStart(2, '0')}/${year}`
})

function toggleOpen(): void {
  isOpen.value = !isOpen.value
}

function selectDay(day: number): void {
  emit('update:modelValue', isoFor(day))
  isOpen.value = false
}

function handleOutsideClick(e: MouseEvent): void {
  if (rootRef.value && !rootRef.value.contains(e.target as Node)) {
    isOpen.value = false
  }
}

onMounted(() => document.addEventListener('click', handleOutsideClick))
onUnmounted(() => document.removeEventListener('click', handleOutsideClick))
</script>

<template>
  <div ref="rootRef" class="date-picker">
    <button type="button" class="date-picker-trigger" @click="toggleOpen">
      <AppIcon name="calendar" :size="13" />
      <span>{{ displayLabel }}</span>
    </button>

    <div v-if="isOpen" class="date-picker-panel">
      <div class="panel-header">{{ monthLabel }}</div>
      <div class="weekday-row">
        <span v-for="w in WEEKDAY_LABELS" :key="w" class="weekday-cell">{{ w }}</span>
      </div>
      <div class="day-grid">
        <span
          v-for="(day, i) in cells"
          :key="i"
          class="day-cell"
          :class="{
            'day-cell--empty':    day === null,
            'day-cell--selected': day !== null && day === selectedDay,
            'day-cell--today':    day !== null && day === todayDay && day !== selectedDay,
          }"
          @click="day !== null && selectDay(day)"
        >{{ day ?? '' }}</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.date-picker {
  position: relative;
}

.date-picker-trigger {
  display: flex;
  align-items: center;
  gap: 6px;
  background: transparent;
  border: none;
  color: var(--color-text);
  font-size: 12px;
  font-family: inherit;
  cursor: pointer;
  padding: 2px 0;
  white-space: nowrap;
}

.date-picker-trigger :deep(svg) {
  color: var(--color-text-muted);
  flex-shrink: 0;
}

.date-picker-panel {
  position: absolute;
  top: calc(100% + 6px);
  left: 0;
  z-index: 20;
  background: var(--color-surface-2);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 10px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.35);
  width: 220px;
}

.panel-header {
  text-align: center;
  font-size: 12px;
  font-weight: 700;
  color: var(--color-cream);
  padding-bottom: 8px;
  text-transform: capitalize;
}

.weekday-row {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  margin-bottom: 4px;
}

.weekday-cell {
  text-align: center;
  font-size: 9.5px;
  font-weight: 700;
  color: var(--color-text-muted);
  text-transform: uppercase;
}

.day-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 2px;
}

.day-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 26px;
  border-radius: var(--radius-sm);
  font-size: 11.5px;
  color: var(--color-text);
  cursor: pointer;
  transition: background 0.12s, color 0.12s;
}

.day-cell:hover:not(.day-cell--empty) {
  background: rgba(242, 141, 53, 0.15);
}

.day-cell--empty {
  cursor: default;
}

.day-cell--today {
  border: 1px solid var(--color-primary);
  color: var(--color-primary);
}

.day-cell--selected {
  background: var(--color-primary);
  color: #0D0D0D;
  font-weight: 700;
}
</style>

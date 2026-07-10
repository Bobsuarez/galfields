<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { formatTime, formatDate } from '../../utils/currency'
import { useSalesSyncStatus } from '../../composables/useSalesSyncStatus'
import type { ShortcutKey } from '../../types'

const now = ref(new Date())
let timer: ReturnType<typeof setInterval>

onMounted(() => {
  timer = setInterval(() => { now.value = new Date() }, 1000)
})
onUnmounted(() => clearInterval(timer))

// "Sin sincronizar" here means sales not yet reported to the cloud — not to
// be confused with the "Ventas Pendientes" (F4) shortcut below, which is a
// different concept (held/parked sales waiting to be resumed).
const { pendingCount: pendingSyncCount } = useSalesSyncStatus()

const shortcuts: ShortcutKey[] = [
  { key: 'F1', label: 'Nueva Venta', action: 'nueva-venta' },
  { key: 'F2', label: 'Buscar Producto', action: 'buscar' },
  { key: 'F3', label: 'Guardar Venta', action: 'guardar' },
  { key: 'F4', label: 'Ventas Pendientes', action: 'pendientes' },
  { key: 'F5', label: 'Cobrar', action: 'cobrar' },
]
</script>

<template>
  <footer class="statusbar">
    <div class="shortcuts">
      <div v-for="sc in shortcuts" :key="sc.key" class="shortcut">
        <span class="shortcut-key">{{ sc.key }}</span>
        <span class="shortcut-label">{{ sc.label }}</span>
      </div>
    </div>
    <div class="statusbar-clock">
      <span v-if="pendingSyncCount > 0" class="sync-badge" :title="`${pendingSyncCount} venta(s) sin sincronizar con la nube`">
        <span class="sync-dot"></span>
        {{ pendingSyncCount }} sin sincronizar
      </span>
      <span class="clock-date">{{ formatDate(now) }}</span>
      <span class="clock-time">{{ formatTime(now) }}</span>
    </div>
  </footer>
</template>

<style scoped>
.statusbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: var(--statusbar-height);
  background: #080808;
  border-top: 1px solid var(--color-border);
  padding: 0 16px;
}

.shortcuts {
  display: flex;
  align-items: center;
  gap: 4px;
}

.shortcut {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 3px 8px;
  border-radius: 4px;
  cursor: pointer;
  transition: background 0.15s;
}

.shortcut:hover {
  background: var(--color-surface-2);
}

.shortcut-key {
  font-size: 10px;
  font-weight: 700;
  color: #0D0D0D;
  background: var(--color-primary);
  padding: 1px 5px;
  border-radius: 3px;
  letter-spacing: 0.3px;
}

.shortcut-label {
  font-size: 11px;
  color: var(--color-text-muted);
}

.statusbar-clock {
  display: flex;
  align-items: center;
  gap: 10px;
}

.sync-badge {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 2px 8px;
  border-radius: 10px;
  background: rgba(255, 152, 0, 0.15);
  color: #FFB74D;
  font-size: 10.5px;
  font-weight: 600;
}

.sync-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #FFB74D;
  flex-shrink: 0;
}

.clock-date {
  font-size: 11px;
  color: var(--color-text-muted);
}

.clock-time {
  font-size: 12px;
  font-weight: 600;
  color: var(--color-cream);
  font-variant-numeric: tabular-nums;
}
</style>

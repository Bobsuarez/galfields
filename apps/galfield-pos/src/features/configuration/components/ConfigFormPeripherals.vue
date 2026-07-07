<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { invoke } from '@tauri-apps/api/core'
import type { ConfigSettings } from '../../../types'

const props = defineProps<{ settings: ConfigSettings }>()

// ── Port lists fetched from Rust ──────────────────────────────────────────────

const serialPorts  = ref<string[]>([])
const videoDevices = ref<string[]>([])
const isScanning   = ref(false)

async function scanPorts() {
  isScanning.value = true
  try {
    const [serial, video] = await Promise.all([
      invoke<string[]>('list_serial_ports'),
      invoke<string[]>('list_video_devices'),
    ])
    serialPorts.value  = serial
    videoDevices.value = video
  } catch (e) {
    console.error('[peripherals] port scan failed:', e)
  } finally {
    isScanning.value = false
  }
}

onMounted(scanPorts)

// ── Device definitions ────────────────────────────────────────────────────────

interface DeviceDef {
  key:   keyof ConfigSettings['peripherals']
  label: string
  emoji: string
  hint:  string
  type:  'serial' | 'video'
}

const DEVICES: DeviceDef[] = [
  { key: 'printerPort',     label: 'Impresora',           emoji: '🖨️', type: 'serial', hint: 'Puerto serie/USB de la impresora térmica' },
  { key: 'barcodePort',     label: 'Lector de Código',    emoji: '📷', type: 'serial', hint: 'Puerto del escáner de código de barras'   },
  { key: 'cashDrawerPort',  label: 'Caja Registradora',   emoji: '💰', type: 'serial', hint: 'Puerto del cajón de dinero'               },
  { key: 'cameraDevice',    label: 'Cámara',              emoji: '📹', type: 'video',  hint: 'Dispositivo de video / cámara IP'          },
  { key: 'fingerprintPort', label: 'Lector de Huella',    emoji: '👆', type: 'serial', hint: 'Puerto del lector biométrico'             },
]

// ── Conflict detection ────────────────────────────────────────────────────────

/**
 * Returns a map of port → device keys that share it.
 * A port is a conflict only when TWO OR MORE devices use it.
 */
const conflictMap = computed(() => {
  const usage = new Map<string, string[]>()
  for (const dev of DEVICES) {
    const port = props.settings.peripherals[dev.key]
    if (!port) continue
    const list = usage.get(port) ?? []
    list.push(dev.key)
    usage.set(port, list)
  }
  const conflicts = new Map<string, boolean>()
  for (const [, keys] of usage) {
    if (keys.length > 1) keys.forEach(k => conflicts.set(k, true))
  }
  return conflicts
})

/** Ports taken by OTHER devices (used to flag duplicates in dropdown). */
function takenByOthers(currentKey: keyof ConfigSettings['peripherals']): Set<string> {
  const taken = new Set<string>()
  for (const dev of DEVICES) {
    if (dev.key === currentKey) continue
    const port = props.settings.peripherals[dev.key]
    if (port) taken.add(port)
  }
  return taken
}

function availableOptions(dev: DeviceDef): string[] {
  return dev.type === 'serial' ? serialPorts.value : videoDevices.value
}

function statusLabel(dev: DeviceDef): { text: string; cls: string } {
  const port = props.settings.peripherals[dev.key]
  if (conflictMap.value.get(dev.key)) return { text: '⚠ Conflicto',    cls: 'badge--warn'  }
  if (port)                            return { text: '✓ Asignado',      cls: 'badge--ok'    }
  return                                      { text: 'Sin asignar',     cls: 'badge--empty' }
}
</script>

<template>
  <div class="periph-form">
    <div class="periph-header">
      <h2 class="section-title">Periféricos</h2>
      <p class="section-desc">Asigna cada dispositivo a su puerto de conexión detectado en el sistema.</p>
      <button class="scan-btn" :disabled="isScanning" @click="scanPorts">
        <span :class="{ spinning: isScanning }">🔄</span>
        {{ isScanning ? 'Escaneando…' : 'Reescanear puertos' }}
      </button>
    </div>

    <!-- Detected ports summary -->
    <div class="ports-summary">
      <div class="port-stat">
        <span class="port-stat-val">{{ serialPorts.length }}</span>
        <span class="port-stat-lbl">Puertos serie</span>
      </div>
      <div class="port-stat-divider"></div>
      <div class="port-stat">
        <span class="port-stat-val">{{ videoDevices.length }}</span>
        <span class="port-stat-lbl">Cámaras</span>
      </div>
      <div class="port-stat-divider"></div>
      <div class="port-stat">
        <span class="port-stat-val">
          {{ DEVICES.filter(d => settings.peripherals[d.key]).length }} / {{ DEVICES.length }}
        </span>
        <span class="port-stat-lbl">Configurados</span>
      </div>
    </div>

    <!-- Device rows -->
    <div class="device-list">
      <div
        v-for="dev in DEVICES"
        :key="dev.key"
        class="device-card"
        :class="{ 'device-card--conflict': conflictMap.get(dev.key), 'device-card--ok': settings.peripherals[dev.key] && !conflictMap.get(dev.key) }"
      >
        <div class="device-avatar">{{ dev.emoji }}</div>

        <div class="device-info">
          <div class="device-top">
            <span class="device-label">{{ dev.label }}</span>
            <span class="device-badge" :class="statusLabel(dev).cls">
              {{ statusLabel(dev).text }}
            </span>
          </div>
          <span class="device-hint">{{ dev.hint }}</span>
        </div>

        <div class="device-select-wrap">
          <select
            v-model="settings.peripherals[dev.key]"
            class="device-select"
            :class="{ 'device-select--conflict': conflictMap.get(dev.key) }"
          >
            <option value="">— Sin asignar —</option>

            <!-- Ports detected by the system -->
            <optgroup v-if="availableOptions(dev).length" label="Detectados">
              <option
                v-for="port in availableOptions(dev)"
                :key="port"
                :value="port"
                :disabled="takenByOthers(dev.key).has(port)"
              >
                {{ port }}{{ takenByOthers(dev.key).has(port) ? '  ✗ En uso' : '' }}
              </option>
            </optgroup>

            <!-- Fallback: if current value is saved but no longer in the detected list -->
            <optgroup
              v-if="settings.peripherals[dev.key] && !availableOptions(dev).includes(settings.peripherals[dev.key])"
              label="Guardado (desconectado)"
            >
              <option :value="settings.peripherals[dev.key]">
                {{ settings.peripherals[dev.key] }}  ⚠ no detectado
              </option>
            </optgroup>

            <optgroup v-if="!availableOptions(dev).length" label="Sin dispositivos">
              <option disabled value="">No hay puertos disponibles</option>
            </optgroup>
          </select>
        </div>

        <!-- Conflict warning -->
        <div v-if="conflictMap.get(dev.key)" class="conflict-msg">
          ⚠ Este puerto ya está asignado a otro dispositivo. Selecciona uno diferente.
        </div>
      </div>
    </div>

    <!-- Printer-specific: paper width, used to size the printed ticket's columns -->
    <div class="paper-width-field">
      <label class="field-label">Ancho de Papel de la Impresora</label>
      <select v-model="settings.peripherals.printerPaperWidth" class="device-select">
        <option value="58mm">58mm (32 columnas)</option>
        <option value="80mm">80mm (48 columnas)</option>
      </select>
    </div>

    <p class="save-note">
      Los cambios se aplican al guardar con el botón <strong>Guardar Cambios</strong>.
    </p>
  </div>
</template>

<style scoped>
.periph-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* ── Header ─────────────────────────────────────────────── */
.periph-header {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.section-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--color-cream);
}

.section-desc {
  font-size: 12px;
  color: var(--color-text-muted);
  line-height: 1.5;
}

.scan-btn {
  align-self: flex-start;
  margin-top: 6px;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  background: transparent;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  color: var(--color-text-muted);
  font-size: 11.5px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
  font-family: inherit;
}

.scan-btn:hover:not(:disabled) {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.scan-btn:disabled { opacity: 0.5; cursor: not-allowed; }

@keyframes spin {
  to { transform: rotate(360deg); }
}
.spinning { display: inline-block; animation: spin 0.8s linear infinite; }

/* ── Summary bar ─────────────────────────────────────────── */
.ports-summary {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 16px;
  background: var(--color-surface-3);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
}

.port-stat {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  flex: 1;
}

.port-stat-val {
  font-size: 20px;
  font-weight: 800;
  color: var(--color-primary);
  font-variant-numeric: tabular-nums;
  line-height: 1;
}

.port-stat-lbl {
  font-size: 9.5px;
  font-weight: 700;
  color: var(--color-text-dim);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.port-stat-divider {
  width: 1px;
  height: 28px;
  background: var(--color-border);
}

/* ── Device list ─────────────────────────────────────────── */
.device-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.device-card {
  display: grid;
  grid-template-columns: 48px 1fr auto;
  align-items: center;
  gap: 14px;
  padding: 14px 16px;
  background: var(--color-surface-2);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  transition: border-color 0.15s;
  position: relative;
}

.device-card--ok       { border-color: rgba(56, 142, 60, 0.35); }
.device-card--conflict { border-color: rgba(229, 57, 53, 0.45); }

.device-avatar {
  font-size: 26px;
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(242, 141, 53, 0.08);
  border-radius: var(--radius-md);
  flex-shrink: 0;
  line-height: 1;
}

.device-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.device-top {
  display: flex;
  align-items: center;
  gap: 8px;
}

.device-label {
  font-size: 13px;
  font-weight: 700;
  color: var(--color-cream);
}

.device-badge {
  font-size: 9.5px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.4px;
  padding: 2px 7px;
  border-radius: 20px;
}

.badge--ok    { background: rgba(56,142,60,0.18);  color: #81c784; }
.badge--warn  { background: rgba(229,57,53,0.18);  color: #e57373; }
.badge--empty { background: var(--color-surface-3); color: var(--color-text-dim); }

.device-hint {
  font-size: 11px;
  color: var(--color-text-dim);
  line-height: 1.4;
}

/* ── Select ──────────────────────────────────────────────── */
.device-select-wrap {
  min-width: 200px;
}

.device-select {
  width: 100%;
  background: var(--color-surface-3);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  color: var(--color-text);
  font-size: 12px;
  padding: 8px 10px;
  font-family: inherit;
  cursor: pointer;
  transition: border-color 0.15s;
  appearance: auto;
}

.device-select:focus          { border-color: var(--color-primary); outline: none; }
.device-select--conflict      { border-color: rgba(229, 57, 53, 0.6); }

/* ── Paper width field ───────────────────────────────────── */
.paper-width-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 14px 16px;
  background: var(--color-surface-2);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
}

.field-label {
  font-size: 11px;
  font-weight: 600;
  color: var(--color-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.4px;
}

/* ── Conflict message ────────────────────────────────────── */
.conflict-msg {
  grid-column: 1 / -1;
  font-size: 11px;
  color: #e57373;
  background: rgba(229, 57, 53, 0.08);
  border: 1px solid rgba(229, 57, 53, 0.2);
  border-radius: var(--radius-sm);
  padding: 6px 10px;
  margin-top: -4px;
}

/* ── Save note ───────────────────────────────────────────── */
.save-note {
  font-size: 11.5px;
  color: var(--color-text-dim);
  text-align: center;
  padding-top: 4px;
}

.save-note strong { color: var(--color-primary); }
</style>

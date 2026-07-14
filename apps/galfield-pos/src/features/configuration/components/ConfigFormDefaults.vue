<script setup lang="ts">
import { onMounted } from 'vue'
import { open } from '@tauri-apps/plugin-dialog'
import type { ConfigSettings } from '../../../types'
import { usePaymentMethods } from '../../../composables/usePaymentMethods'
import AppToggle from '../../../components/shared/AppToggle.vue'

const props = defineProps<{ settings: ConfigSettings }>()

const { paymentMethods, loadPaymentMethods } = usePaymentMethods()
onMounted(loadPaymentMethods)

async function browseInvoiceFolder() {
  const folder = await open({ directory: true, multiple: false })
  if (typeof folder === 'string') {
    props.settings.defaults.invoiceArchiveFolder = folder
  }
}
</script>

<template>
  <div class="col-forms">
    <section class="form-card">
      <h3 class="card-title">Configuraciones por Defecto</h3>

      <div class="field">
        <label class="field-label">Vendedor Predeterminado</label>
        <select v-model="settings.defaults.seller" class="field-input">
          <option value="Admin">Admin</option>
          <option value="Cajero 1">Cajero 1</option>
          <option value="Cajero 2">Cajero 2</option>
        </select>
      </div>
      <div class="field">
        <label class="field-label">Consumidor Final</label>
        <input v-model="settings.defaults.customer" class="field-input" placeholder="Consumidor Final" />
      </div>
      <div class="field">
        <label class="field-label">Categoría Principal</label>
        <select v-model="settings.defaults.mainCategory" class="field-input">
          <option value="bebidas">Bebidas</option>
          <option value="alimentos">Alimentos</option>
          <option value="snacks">Snacks</option>
          <option value="lacteos">Lácteos</option>
          <option value="limpieza">Limpieza</option>
          <option value="otros">Otros</option>
        </select>
      </div>
      <div class="field-row">
        <div class="field">
          <label class="field-label">Método de Pago</label>
          <select v-model="settings.defaults.paymentMethod" class="field-input">
            <option v-for="method in paymentMethods" :key="method.id" :value="method.name">
              {{ method.name }}
            </option>
          </select>
        </div>
        <div class="field">
          <label class="field-label">Política de Impuesto</label>
          <select v-model="settings.defaults.taxPolicy" class="field-input">
            <option value="Sin IVA">Sin IVA</option>
            <option value="IVA 19%">IVA 19%</option>
            <option value="IVA 8%">IVA 8%</option>
          </select>
        </div>
      </div>

      <div class="field">
        <label class="field-label">Carpeta de Archivo de Facturas (PDF)</label>
        <div class="field-suffix-wrap">
          <input
            :value="settings.defaults.invoiceArchiveFolder || 'Sin configurar'"
            readonly
            class="field-input"
          />
          <button type="button" class="browse-btn" @click="browseInvoiceFolder">Examinar…</button>
        </div>
      </div>

      <div class="toggles-group">
        <AppToggle v-model="settings.defaults.printReceipt" label="Imprimir recibo de cada venta" />
        <AppToggle v-model="settings.defaults.emailReceipt" label="Enviar email de recibo por cada venta" />
        <AppToggle v-model="settings.defaults.roundPrices" label="Redondear precios al centavo más cercano" />
        <AppToggle v-model="settings.defaults.emailNotifications" label="Notificación por email en transacciones" />
        <AppToggle v-model="settings.defaults.validateStock" label="Validar stock (sin stock no permite venta)" />
      </div>
    </section>

    <section class="form-card">
      <h3 class="card-title">Reglas y Sincronización</h3>

      <div class="field">
        <label class="field-label">URL del servidor (nube)</label>
        <input
          v-model.trim="settings.sync.apiBaseUrl"
          type="url"
          class="field-input"
          placeholder="https://galfields.kinforgeworks.com"
        />
        <p class="field-hint">A dónde se conecta esta terminal para sincronizar catálogo y reportar ventas. Solo cámbialo si sabes lo que haces — un valor incorrecto rompe la sincronización.</p>
      </div>
      <div class="field">
        <label class="field-label">Intervalo de Respaldo</label>
        <select v-model="settings.sync.backupInterval" class="field-input">
          <option value="15min">Cada 15 minutos</option>
          <option value="30min">Cada 30 minutos</option>
          <option value="1h">Cada hora</option>
          <option value="6h">Cada 6 horas</option>
          <option value="24h">Una vez al día</option>
        </select>
      </div>
      <div class="field">
        <label class="field-label">Sincronizar precios cada</label>
        <div class="field-suffix-wrap">
          <input v-model.number="settings.sync.priceSyncHours" type="number" min="1" max="72" class="field-input" />
          <span class="field-suffix">horas</span>
        </div>
      </div>
      <div class="field">
        <label class="field-label">Reintentar ventas pendientes cada</label>
        <div class="field-suffix-wrap">
          <input v-model.number="settings.sync.salesRetryMinutes" type="number" min="1" max="60" class="field-input" />
          <span class="field-suffix">minutos</span>
        </div>
      </div>
      <div class="field">
        <label class="field-label">Prefijo de Facturas</label>
        <input v-model="settings.sync.invoicePrefix" class="field-input" placeholder="FAC-" />
      </div>
      <button class="backup-btn">
        💾 Guardar Respaldo Ahora
      </button>
    </section>
  </div>
</template>

<style scoped>
.col-forms {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-card {
  background: var(--color-surface-2);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.card-title {
  font-size: 13px;
  font-weight: 700;
  color: var(--color-cream);
  padding-bottom: 8px;
  border-bottom: 1px solid var(--color-border);
}

.field {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.field-label {
  font-size: 11px;
  font-weight: 600;
  color: var(--color-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.4px;
}

.field-hint {
  font-size: 11px;
  color: var(--color-text-muted);
  margin: 0;
}

.field-input {
  background: var(--color-surface-3);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  color: var(--color-text);
  font-size: 12.5px;
  padding: 7px 10px;
  font-family: inherit;
  transition: border-color 0.2s;
}

.field-input:focus { border-color: var(--color-primary); outline: none; }

.field-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.field-suffix-wrap {
  display: flex;
  align-items: center;
  background: var(--color-surface-3);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  overflow: hidden;
  transition: border-color 0.2s;
}

.field-suffix-wrap:focus-within { border-color: var(--color-primary); }

.field-suffix-wrap .field-input {
  background: transparent;
  border: none;
  border-radius: 0;
  flex: 1;
}

.field-suffix {
  font-size: 11px;
  color: var(--color-text-muted);
  padding: 0 10px;
  background: rgba(242,141,53,0.06);
  border-left: 1px solid var(--color-border);
  height: 100%;
  display: flex;
  align-items: center;
  flex-shrink: 0;
  white-space: nowrap;
}

.browse-btn {
  flex-shrink: 0;
  padding: 0 12px;
  height: 100%;
  background: rgba(242,141,53,0.1);
  border: none;
  border-left: 1px solid var(--color-border);
  color: var(--color-primary);
  font-size: 11.5px;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
  transition: background 0.15s;
}

.browse-btn:hover { background: rgba(242,141,53,0.2); }

.toggles-group {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 4px 0;
  border-top: 1px solid var(--color-border);
}

.backup-btn {
  padding: 9px;
  background: rgba(242,141,53,0.1);
  border: 1px solid rgba(242,141,53,0.3);
  border-radius: var(--radius-sm);
  color: var(--color-primary);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s;
  text-align: center;
}

.backup-btn:hover { background: rgba(242,141,53,0.2); }
</style>

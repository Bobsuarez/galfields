<script setup lang="ts">
import type { ConfigSettings } from '../../../types'

defineProps<{ settings: ConfigSettings }>()
</script>

<template>
  <div class="col-forms">
    <section class="form-card">
      <h3 class="card-title">Configuraciones Generales</h3>

      <div class="field">
        <label class="field-label">Nombre Tienda</label>
        <input v-model="settings.general.storeName" class="field-input" placeholder="Mi Tienda" />
      </div>
      <div class="field">
        <label class="field-label">RFC / Cédula DIAN</label>
        <input v-model="settings.general.taxId" class="field-input" placeholder="900000000-0" />
      </div>
      <div class="field-row">
        <div class="field">
          <label class="field-label">Idioma</label>
          <select v-model="settings.general.language" class="field-input">
            <option value="es">Español</option>
            <option value="en">English</option>
          </select>
        </div>
        <div class="field">
          <label class="field-label">Moneda</label>
          <select v-model="settings.general.currency" class="field-input">
            <option value="COP">COP — Peso Colombiano</option>
            <option value="USD">USD — Dólar</option>
            <option value="MXN">MXN — Peso Mexicano</option>
          </select>
        </div>
      </div>
      <div class="field">
        <label class="field-label">Zona Horaria</label>
        <select v-model="settings.general.timezone" class="field-input">
          <option value="America/Bogota">América/Bogotá (GMT-5)</option>
          <option value="America/Mexico_City">América/Mexico City (GMT-6)</option>
          <option value="America/Lima">América/Lima (GMT-5)</option>
          <option value="America/Santiago">América/Santiago (GMT-3)</option>
        </select>
      </div>
      <div class="field">
        <label class="field-label">Formato de Fecha</label>
        <select v-model="settings.general.dateFormat" class="field-input">
          <option value="DD/MM/YYYY">DD/MM/YYYY</option>
          <option value="MM/DD/YYYY">MM/DD/YYYY</option>
          <option value="YYYY-MM-DD">YYYY-MM-DD</option>
        </select>
      </div>
    </section>

    <section class="form-card">
      <h3 class="card-title">Preferencias de la Tienda</h3>

      <div class="field">
        <label class="field-label">Nombre Comercial</label>
        <input v-model="settings.store.name" class="field-input" placeholder="Nombre de tu tienda" />
      </div>
      <div class="field">
        <label class="field-label">Dirección</label>
        <input v-model="settings.store.address" class="field-input" placeholder="Calle 123 # 45-67" />
      </div>
      <div class="field-row">
        <div class="field">
          <label class="field-label">Teléfono</label>
          <input v-model="settings.store.phone" class="field-input" placeholder="+57 300 000 0000" />
        </div>
        <div class="field">
          <label class="field-label">Correo</label>
          <input v-model="settings.store.email" type="email" class="field-input" placeholder="tienda@email.com" />
        </div>
      </div>
      <div class="field">
        <label class="field-label">Logo de la Tienda</label>
        <div class="logo-upload">
          <div class="logo-preview">🏪</div>
          <div class="logo-info">
            <span class="logo-name">{{ settings.store.name || 'Sin logo' }}</span>
            <button class="logo-btn">Subir logo</button>
          </div>
        </div>
      </div>
      <div class="field">
        <label class="field-label">Eslogan</label>
        <input v-model="settings.store.slogan" class="field-input" placeholder="Tu punto de venta inteligente" />
      </div>
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

.logo-upload {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px;
  background: var(--color-surface-3);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
}

.logo-preview {
  font-size: 28px;
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(242,141,53,0.12);
  border-radius: 8px;
  border: 1px solid rgba(242,141,53,0.2);
}

.logo-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.logo-name {
  font-size: 12px;
  font-weight: 500;
  color: var(--color-cream);
}

.logo-btn {
  font-size: 11px;
  padding: 3px 10px;
  background: rgba(242,141,53,0.12);
  border: 1px solid rgba(242,141,53,0.3);
  border-radius: var(--radius-sm);
  color: var(--color-primary);
  cursor: pointer;
  transition: background 0.15s;
}

.logo-btn:hover { background: rgba(242,141,53,0.2); }
</style>

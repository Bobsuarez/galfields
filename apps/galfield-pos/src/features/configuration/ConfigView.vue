<script setup lang="ts">
import { useConfig, CONFIG_TABS, COLOR_PRESETS } from './composables/useConfig'
import ConfigFormGeneral     from './components/ConfigFormGeneral.vue'
import ConfigFormDefaults    from './components/ConfigFormDefaults.vue'
import ConfigFormStyles      from './components/ConfigFormStyles.vue'
import ConfigFormPeripherals from './components/ConfigFormPeripherals.vue'

const { activeTab, settings, isLoading, isSaving, setTab, applyPreset, save } = useConfig()
</script>

<template>
  <div class="config-view">
    <div class="config-header">
      <div class="config-title-area">
        <h1 class="config-title">Configuración</h1>
        <p class="config-subtitle">Personaliza y ajusta tu sistema</p>
      </div>
      <button class="save-btn" :disabled="isSaving || isLoading" @click="save">
        {{ isSaving ? '⏳ Guardando…' : '💾 Guardar Cambios' }}
      </button>
    </div>

    <div class="config-tabs">
      <button
        v-for="tab in CONFIG_TABS"
        :key="tab.id"
        class="tab-btn"
        :class="{ 'tab-btn--active': activeTab === tab.id }"
        @click="setTab(tab.id)"
      >
        {{ tab.label }}
      </button>
    </div>

    <div class="config-content">
      <div v-if="isLoading" class="loading-state">Cargando configuración…</div>

      <template v-else-if="activeTab === 'empresa'">
        <div class="config-grid">
          <ConfigFormGeneral :settings="settings" />
          <ConfigFormDefaults :settings="settings" />
          <ConfigFormStyles
            :settings="settings"
            :color-presets="COLOR_PRESETS"
            @apply-preset="applyPreset"
          />
        </div>
      </template>

      <template v-else-if="activeTab === 'perifericos'">
        <ConfigFormPeripherals :settings="settings" />
      </template>

      <template v-else-if="!isLoading">
        <div class="stub-tab">
          <span class="stub-icon">⚙️</span>
          <h2 class="stub-title">{{ CONFIG_TABS.find(t => t.id === activeTab)?.label }}</h2>
          <p class="stub-desc">Sección en construcción</p>
        </div>
      </template>
    </div>
  </div>
</template>

<style scoped>
.config-view {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
  background: var(--color-surface);
}

.config-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 20px 10px;
  border-bottom: 1px solid var(--color-border);
  flex-shrink: 0;
}

.config-title-area {}

.config-title {
  font-size: 18px;
  font-weight: 700;
  color: var(--color-cream);
  line-height: 1.2;
}

.config-subtitle {
  font-size: 12px;
  color: var(--color-text-muted);
  margin-top: 2px;
}

.save-btn {
  padding: 9px 20px;
  background: var(--color-primary);
  border: none;
  border-radius: var(--radius-sm);
  color: #0D0D0D;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  transition: background 0.15s, transform 0.1s;
}

.save-btn:hover:not(:disabled) { background: var(--color-primary-hover); transform: translateY(-1px); }
.save-btn:disabled { opacity: 0.55; cursor: not-allowed; }

.loading-state {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 200px;
  color: var(--color-text-muted);
  font-size: 13px;
}

.config-tabs {
  display: flex;
  gap: 2px;
  padding: 10px 20px 0;
  border-bottom: 1px solid var(--color-border);
  flex-shrink: 0;
  overflow-x: auto;
}

.config-tabs::-webkit-scrollbar { height: 2px; }

.tab-btn {
  padding: 8px 14px;
  background: transparent;
  border: none;
  border-bottom: 2px solid transparent;
  color: var(--color-text-muted);
  font-size: 12.5px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s;
  white-space: nowrap;
  margin-bottom: -1px;
}

.tab-btn:hover { color: var(--color-cream); }

.tab-btn--active {
  color: var(--color-primary);
  border-bottom-color: var(--color-primary);
  font-weight: 700;
}

.config-content {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}

.config-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  align-items: start;
}

.stub-tab {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  min-height: 300px;
  gap: 12px;
  color: var(--color-text-muted);
}

.stub-icon { font-size: 48px; }
.stub-title { font-size: 20px; font-weight: 700; color: var(--color-cream); }
.stub-desc { font-size: 13px; }
</style>

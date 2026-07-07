<script setup lang="ts">
import AppIcon from "./AppIcon.vue";
</script>

<template>
  <header class="topbar">
    <div class="brand-logo">
        <img
          src="/src/assets/images/logo.png"
          alt="Descripción"
          width="150"
          height="55"
        />
    </div>

    <div class="topbar-actions">
      <button class="action-btn" title="Notificaciones">
        <AppIcon name="bell" :size="18" />
        <span class="badge">2</span>
      </button>
      <div class="user-info">
        <div class="user-avatar">
          <AppIcon name="user" :size="16" />
        </div>
        <div class="user-meta">
          <span class="user-name">Cajero</span>
          <span class="user-role">Administrador</span>
        </div>
      </div>
    </div>
  </header>
</template>

<style scoped>
.topbar {
  display: flex;
  align-items: center;
  height: var(--topbar-height);
  background: #080808;
  border-bottom: 1px solid var(--color-border);
  padding: 0 16px;
  gap: 12px;
}

.topbar-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  width: calc(var(--sidebar-width) - 16px);
  flex-shrink: 0;
}

.brand-logo {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 11.5%;
  height: 15px;
  border-radius: 8px;
}

.brand-text {
  display: flex;
  flex-direction: column;
  line-height: 1.2;
}

.brand-name {
  font-size: 15px;
  font-weight: 700;
  color: var(--color-primary);
  letter-spacing: 0.5px;
}

.brand-subtitle {
  font-size: 10px;
  color: var(--color-text-muted);
  font-weight: 400;
}

.topbar-search {
  flex: 1;
  display: flex;
  align-items: center;
  background: var(--color-surface-2);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 0 12px;
  gap: 8px;
  max-width: 520px;
  transition: border-color 0.2s;
}

.topbar-search:focus-within {
  border-color: var(--color-primary);
}

.search-icon {
  color: var(--color-text-muted);
  flex-shrink: 0;
}

.search-input {
  flex: 1;
  background: transparent;
  border: none;
  color: var(--color-text);
  padding: 8px 0;
  font-size: 13px;
}

.search-input::placeholder {
  color: var(--color-text-dim);
}

.topbar-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-left: auto;
}

.action-btn {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  background: var(--color-surface-2);
  border-radius: var(--radius-sm);
  color: var(--color-text-muted);
  border: 1px solid var(--color-border);
  transition:
    color 0.2s,
    border-color 0.2s;
}

.action-btn:hover {
  color: var(--color-primary);
  border-color: var(--color-primary);
}

.badge {
  position: absolute;
  top: -4px;
  right: -4px;
  background: var(--color-primary);
  color: #0d0d0d;
  font-size: 9px;
  font-weight: 700;
  width: 15px;
  height: 15px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: var(--radius-sm);
  border: 1px solid var(--color-border);
  background: var(--color-surface-2);
  transition: border-color 0.2s;
}

.user-info:hover {
  border-color: var(--color-primary);
}

.user-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: rgba(242, 141, 53, 0.2);
  border: 1px solid rgba(242, 141, 53, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-primary);
}

.user-meta {
  display: flex;
  flex-direction: column;
  line-height: 1.2;
}

.user-name {
  font-size: 12px;
  font-weight: 600;
  color: var(--color-text);
}

.user-role {
  font-size: 10px;
  color: var(--color-text-muted);
}
</style>

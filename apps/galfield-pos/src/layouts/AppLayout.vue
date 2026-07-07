<script setup lang="ts">
import AppSidebar from '../components/shared/AppSidebar.vue'
import AppTopbar from '../components/shared/AppTopbar.vue'
import AppStatusbar from '../components/shared/AppStatusbar.vue'
</script>

<template>
  <div class="app-layout">
    <AppTopbar />
    <div class="app-body">
      <AppSidebar />
      <main class="app-content">
        <slot />
      </main>
    </div>
    <AppStatusbar />
  </div>
</template>

<style scoped>
.app-layout {
  display: flex;
  flex-direction: column;
  height: 100vh;
  overflow: hidden;
  background: var(--color-bg);
}

.app-body {
  flex: 1;
  display: flex;
  overflow: hidden;
  min-height: 0;
}

.app-content {
  flex: 1;
  overflow: hidden;
  min-width: 0;
  background: var(--color-surface);
}
</style>

<script setup lang="ts">
import { onMounted } from 'vue'
import AppLayout from './layouts/AppLayout.vue'
import AppToast from './components/shared/AppToast.vue'
import { useAppConfig } from './composables/useAppConfig'

const { loadConfig } = useAppConfig()
onMounted(loadConfig)
</script>

<template>
  <AppLayout>
    <router-view />
  </AppLayout>
  <AppToast />
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { RouterView, useRoute } from 'vue-router'
import AppShell from '@/components/layout/AppShell.vue'

const route = useRoute()
const usesBlankLayout = computed(() => route.meta.layout === 'blank')
</script>

<template>
  <RouterView v-if="usesBlankLayout" />
  <AppShell v-else />
</template>

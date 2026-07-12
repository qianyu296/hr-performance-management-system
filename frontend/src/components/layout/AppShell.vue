<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import SidebarNavigation from './SidebarNavigation.vue'
import TopHeader from './TopHeader.vue'
const collapsed = ref(localStorage.getItem('hrpm-nav-collapsed') === 'true')
const mobile = ref(false)
const mobileExpanded = ref(false)
const effectiveCollapsed = computed(() => mobile.value ? !mobileExpanded.value : collapsed.value)
function syncViewport() { mobile.value = window.innerWidth <= 900; if (!mobile.value) mobileExpanded.value = false }
function toggle() {
  if (mobile.value) { mobileExpanded.value = !mobileExpanded.value; return }
  collapsed.value = !collapsed.value
  localStorage.setItem('hrpm-nav-collapsed', String(collapsed.value))
}
onMounted(() => { syncViewport(); window.addEventListener('resize', syncViewport) })
onBeforeUnmount(() => window.removeEventListener('resize', syncViewport))
</script>
<template><div class="app-shell" :class="{ 'is-collapsed': effectiveCollapsed }"><SidebarNavigation :collapsed="effectiveCollapsed" /><main class="main-area"><TopHeader :collapsed="effectiveCollapsed" @toggle="toggle" /><div class="route-area"><RouterView /></div></main></div></template>

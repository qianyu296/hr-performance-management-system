<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { visibleNavigationItems } from '@/router/navigation'
import { useAuthStore } from '@/stores/auth'

defineProps<{ collapsed: boolean }>()

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const items = computed(() => visibleNavigationItems(authStore.permissions))
const activePath = computed(() => {
  if (route.path.startsWith('/org/') || route.path.startsWith('/personnel/')) return '/people'
  if (route.path.startsWith('/attendance/')) return '/attendance'
  if (route.path.startsWith('/workflow/')) return '/workflow'
  return route.path
})
</script>

<template>
  <aside class="sidebar" :class="{ collapsed }">
    <div class="brand">
      <span class="brand-mark">H</span>
      <span v-show="!collapsed">HRPM</span>
    </div>
    <el-menu :default-active="activePath" :collapse="collapsed" :collapse-transition="false" @select="router.push">
      <el-menu-item v-for="item in items" :key="item.path" :index="item.path">
        <el-icon><component :is="item.icon" /></el-icon>
        <template #title>{{ item.title }}</template>
      </el-menu-item>
    </el-menu>
  </aside>
</template>

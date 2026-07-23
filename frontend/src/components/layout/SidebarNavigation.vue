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
</script>

<template>
  <aside class="sidebar" :class="{ collapsed }">
    <div class="brand">
      <span class="brand-mark">H</span>
      <span v-show="!collapsed">HRPM</span>
    </div>
    <el-menu :default-active="route.path" :collapse="collapsed" :collapse-transition="false" @select="router.push">
      <template v-for="item in items" :key="item.path">
        <el-sub-menu v-if="item.children" :index="item.path">
          <template #title>
            <el-icon v-if="item.icon"><component :is="item.icon" /></el-icon>
            <span>{{ item.title }}</span>
          </template>
          <el-menu-item v-for="child in item.children" :key="child.path" :index="child.path">
            {{ child.title }}
          </el-menu-item>
        </el-sub-menu>
        <el-menu-item v-else :index="item.path">
          <el-icon v-if="item.icon"><component :is="item.icon" /></el-icon>
          <template #title>{{ item.title }}</template>
        </el-menu-item>
      </template>
    </el-menu>
  </aside>
</template>

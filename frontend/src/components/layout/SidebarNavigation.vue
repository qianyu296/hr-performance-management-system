<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { navigationGroups } from '@/router/navigation'
defineProps<{ collapsed: boolean }>()
const route = useRoute(); const router = useRouter(); const activePath = computed(() => route.path)
</script>
<template><aside class="sidebar" :class="{ collapsed }"><div class="brand"><span class="brand-mark">H</span><span v-show="!collapsed">HRPM</span></div><el-menu :default-active="activePath" :collapse="collapsed" :collapse-transition="false" @select="router.push"><template v-for="group in navigationGroups" :key="group.title"><div v-show="!collapsed" class="menu-group">{{ group.title }}</div><el-menu-item v-for="item in group.items" :key="item.path" :index="item.path"><el-icon><component :is="item.icon" /></el-icon><template #title>{{ item.title }}</template></el-menu-item></template></el-menu></aside></template>

<script setup lang="ts">
import { Bell, Expand, Fold, User } from '@element-plus/icons-vue'
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

defineProps<{ collapsed: boolean }>()
const emit = defineEmits<{ toggle: [] }>()
const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const title = computed(() => String(route.meta.title ?? '工作台'))

async function logout() {
  await authStore.signOut()
  await router.replace('/login')
}
</script>

<template>
  <header class="top-header">
    <el-button text class="icon-button" :aria-label="collapsed ? '展开导航' : '收起导航'" @click="emit('toggle')">
      <el-icon :size="20"><component :is="collapsed ? Expand : Fold" /></el-icon>
    </el-button>
    <el-breadcrumb separator="/">
      <el-breadcrumb-item>HRPM</el-breadcrumb-item>
      <el-breadcrumb-item>{{ title }}</el-breadcrumb-item>
    </el-breadcrumb>
    <div class="header-tools">
      <el-tooltip content="通知">
        <el-button text class="icon-button" aria-label="通知">
          <el-icon :size="19"><Bell /></el-icon>
        </el-button>
      </el-tooltip>
      <el-dropdown>
        <button class="account-button" type="button">
          <el-icon><User /></el-icon>
          <span>{{ authStore.displayName }}</span>
        </button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item disabled>个人设置</el-dropdown-item>
            <el-dropdown-item divided @click="logout">退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </header>
</template>

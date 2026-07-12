<script setup lang="ts">import { Document } from '@element-plus/icons-vue'; defineProps<{ title: string; description: string }>();</script>
<template><div class="empty-state"><el-icon :size="30"><Document /></el-icon><strong>{{ title }}</strong><span>{{ description }}</span></div></template>

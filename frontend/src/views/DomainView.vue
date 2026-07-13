<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import PageFrame from '@/components/common/PageFrame.vue'
import EmptyState from '@/components/common/EmptyState.vue'

const route = useRoute()
const title = computed(() => String(route.meta.title ?? '业务模块'))
const description = computed(() => String(route.meta.description ?? '该模块正在接入业务数据。'))
</script>

<template>
  <PageFrame :title="title" :description="description">
    <template #actions>
      <el-button type="primary">新建</el-button>
    </template>
    <template #filters>
      <el-input placeholder="搜索名称、编号或状态" clearable />
      <el-select placeholder="全部状态" clearable>
        <el-option label="全部状态" value="" />
        <el-option label="进行中" value="active" />
        <el-option label="已完成" value="complete" />
      </el-select>
      <el-button>筛选</el-button>
    </template>
    <el-table :data="[]" class="data-table">
      <el-table-column label="名称" min-width="220" />
      <el-table-column label="状态" width="140" />
      <el-table-column label="最近更新" width="180" />
      <el-table-column label="操作" width="120" />
    </el-table>
    <EmptyState :title="title + '暂无记录'" :description="description" />
  </PageFrame>
</template>

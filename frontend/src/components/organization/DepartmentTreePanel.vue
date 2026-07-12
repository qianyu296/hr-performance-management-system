<script setup lang="ts">
import type { DepartmentNode } from '@/types/organization'

defineProps<{ departments: DepartmentNode[]; selectedId?: string; loading: boolean }>()
const emit = defineEmits<{ select: [id?: string] }>()
</script>

<template>
  <aside class="department-panel" aria-label="部门筛选">
    <div class="organization-panel-title">
      <strong>组织架构</strong>
      <el-button link type="primary" @click="emit('select', undefined)">全部</el-button>
    </div>
    <el-skeleton v-if="loading" :rows="6" animated />
    <el-tree
      v-else
      :data="departments"
      node-key="id"
      :props="{ label: 'name', children: 'children' }"
      :current-node-key="selectedId"
      highlight-current
      default-expand-all
      @node-click="(node: DepartmentNode) => emit('select', node.id)"
    />
  </aside>
</template>

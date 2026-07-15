<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { Edit, Plus } from '@element-plus/icons-vue'
import type { CreatePositionPayload, CreateRankPayload, Position, Rank, UpdatePositionPayload, UpdateRankPayload } from '@/types/organization'

const props = defineProps<{ modelValue: boolean; positions: Position[]; ranks: Rank[]; saving: boolean }>()
const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  savePosition: [payload: CreatePositionPayload | UpdatePositionPayload, id?: string]
  saveRank: [payload: CreateRankPayload | UpdateRankPayload, id?: string]
}>()
const tab = ref('positions')
const position = reactive({ id: '', code: '', name: '', jobFamily: '', description: '', sortNo: 0, status: 'ACTIVE', version: '0' })
const rank = reactive({ id: '', code: '', name: '', rankOrder: 0, status: 'ACTIVE', version: '0' })
const resetPosition = () => Object.assign(position, { id: '', code: '', name: '', jobFamily: '', description: '', sortNo: 0, status: 'ACTIVE', version: '0' })
const resetRank = () => Object.assign(rank, { id: '', code: '', name: '', rankOrder: 0, status: 'ACTIVE', version: '0' })
watch(() => props.modelValue, (open) => { if (open) { resetPosition(); resetRank() } })
</script>

<template>
  <el-dialog :model-value="modelValue" title="岗位与职级" width="min(760px, 94vw)" @close="emit('update:modelValue', false)">
    <el-tabs v-model="tab">
      <el-tab-pane label="岗位" name="positions">
        <div class="master-data-layout">
          <el-table :data="positions" max-height="330"><el-table-column prop="code" label="编码" /><el-table-column prop="name" label="名称" /><el-table-column width="64"><template #default="scope"><el-tooltip content="编辑岗位"><el-button :icon="Edit" circle text aria-label="编辑岗位" @click="Object.assign(position, scope.row)" /></el-tooltip></template></el-table-column></el-table>
          <el-form label-position="top"><el-form-item label="编码"><el-input v-model="position.code" :disabled="Boolean(position.id)" /></el-form-item><el-form-item label="名称"><el-input v-model="position.name" /></el-form-item><el-form-item label="岗位序列"><el-input v-model="position.jobFamily" /></el-form-item><el-form-item label="排序"><el-input-number v-model="position.sortNo" :min="0" /></el-form-item><el-button type="primary" :icon="position.id ? Edit : Plus" :loading="saving" @click="emit('savePosition', { code: position.code, name: position.name, jobFamily: position.jobFamily || undefined, description: position.description || undefined, sortNo: position.sortNo, status: position.status, ...(position.id ? { version: position.version } : {}) }, position.id || undefined)">{{ position.id ? '保存岗位' : '新增岗位' }}</el-button></el-form>
        </div>
      </el-tab-pane>
      <el-tab-pane label="职级" name="ranks">
        <div class="master-data-layout">
          <el-table :data="ranks" max-height="330"><el-table-column prop="code" label="编码" /><el-table-column prop="name" label="名称" /><el-table-column width="64"><template #default="scope"><el-tooltip content="编辑职级"><el-button :icon="Edit" circle text aria-label="编辑职级" @click="Object.assign(rank, scope.row)" /></el-tooltip></template></el-table-column></el-table>
          <el-form label-position="top"><el-form-item label="编码"><el-input v-model="rank.code" :disabled="Boolean(rank.id)" /></el-form-item><el-form-item label="名称"><el-input v-model="rank.name" /></el-form-item><el-form-item label="级别顺序"><el-input-number v-model="rank.rankOrder" :min="0" /></el-form-item><el-button type="primary" :icon="rank.id ? Edit : Plus" :loading="saving" @click="emit('saveRank', { code: rank.code, name: rank.name, rankOrder: rank.rankOrder, status: rank.status, ...(rank.id ? { version: rank.version } : {}) }, rank.id || undefined)">{{ rank.id ? '保存职级' : '新增职级' }}</el-button></el-form>
        </div>
      </el-tab-pane>
    </el-tabs>
  </el-dialog>
</template>

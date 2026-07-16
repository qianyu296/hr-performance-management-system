<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessageBox } from 'element-plus'
import type { CreateExitHandoverItemPayload, ExitHandoverItem } from '@/types/personnel'

const props = defineProps<{
  items: ExitHandoverItem[]
  employeesById: Record<string, string>
  editable: boolean
  saving: boolean
}>()

const emit = defineEmits<{
  add: [payload: CreateExitHandoverItemPayload]
  confirm: [itemId: string, version: string, remark?: string]
}>()

const adding = ref(false)
const form = reactive<CreateExitHandoverItemPayload>({ itemType: 'WORK', required: true, receiverEmployeeId: undefined, remark: '' })

function employeeName(id: string | null) {
  return id ? (props.employeesById[id] ?? id) : '-'
}

function resetForm() {
  Object.assign(form, { itemType: 'WORK', required: true, receiverEmployeeId: undefined, remark: '' })
}

function addItem() {
  emit('add', { ...form, receiverEmployeeId: form.receiverEmployeeId || undefined, remark: form.remark || undefined })
  adding.value = false
  resetForm()
}

async function confirmItem(item: ExitHandoverItem) {
  const result = await ElMessageBox.prompt('可选填写确认备注', '确认交接事项', {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    inputPlaceholder: '例如：账号已停用，资料已交接',
    inputValue: item.remark ?? '',
  }).catch(() => null)
  if (!result) return
  emit('confirm', item.id, item.version, result.value || undefined)
}
</script>

<template>
  <section class="handover-section">
    <div class="section-heading">
      <h2>离职交接</h2>
      <el-button v-if="editable" type="primary" link @click="adding = !adding">{{ adding ? '取消新增' : '新增事项' }}</el-button>
    </div>
    <div v-if="adding" class="handover-editor">
      <el-form label-position="top" class="form-grid">
        <el-form-item label="事项类型">
          <el-select v-model="form.itemType">
            <el-option label="工作交接" value="WORK" />
            <el-option label="资产交接" value="ASSET" />
            <el-option label="账号处理" value="ACCOUNT" />
          </el-select>
        </el-form-item>
        <el-form-item label="接收人">
          <el-select v-model="form.receiverEmployeeId" filterable clearable>
            <el-option v-for="(name, id) in employeesById" :key="id" :label="name" :value="id" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" placeholder="补充说明交接内容" />
        </el-form-item>
        <el-form-item label="是否必办">
          <el-switch v-model="form.required" />
        </el-form-item>
      </el-form>
      <div class="handover-editor-actions">
        <el-button @click="adding = false; resetForm()">取消</el-button>
        <el-button type="primary" :loading="saving" @click="addItem">保存事项</el-button>
      </div>
    </div>
    <el-table :data="items" class="data-table" size="small">
      <el-table-column prop="itemType" label="类型" width="110" />
      <el-table-column label="接收人" min-width="150">
        <template #default="scope">{{ employeeName(scope.row.receiverEmployeeId) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="120">
        <template #default="scope">
          <el-tag :type="scope.row.status === 'CONFIRMED' ? 'success' : 'warning'" size="small">
            {{ scope.row.status === 'CONFIRMED' ? '已确认' : '待确认' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="必办" width="90">
        <template #default="scope">{{ scope.row.required ? '是' : '否' }}</template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" min-width="220" />
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="scope">
          <el-button v-if="scope.row.canConfirm" link type="primary" :loading="saving" @click="confirmItem(scope.row)">确认</el-button>
          <span v-else class="handover-meta">{{ employeeName(scope.row.confirmedBy) }}</span>
        </template>
      </el-table-column>
    </el-table>
  </section>
</template>

<style scoped>
.handover-section {
  background: #fff;
  border: 1px solid #e5eaf1;
  border-radius: 8px;
  overflow: hidden;
}

.handover-editor {
  padding: 16px 18px 8px;
  border-bottom: 1px solid #e9edf3;
  background: #fafbfd;
}

.handover-editor-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding-bottom: 8px;
}

.handover-meta {
  color: #7a8796;
  font-size: 12px;
}
</style>

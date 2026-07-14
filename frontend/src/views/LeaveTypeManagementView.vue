<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageFrame from '@/components/common/PageFrame.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import { createLeaveType, disableLeaveType, fetchManagedLeaveTypes, updateLeaveType, type ManagedLeaveType } from '@/api/leave'

const rows = ref<ManagedLeaveType[]>([])
const loading = ref(false); const saving = ref(false); const dialog = ref(false)
const form = reactive({ id: '', code: '', name: '', deductBalance: true, annualQuota: 0, minUnitHours: 1, version: '' })
function reset(row?: ManagedLeaveType) { Object.assign(form, row ? { ...row, annualQuota: row.annualQuota ?? 0 } : { id: '', code: '', name: '', deductBalance: true, annualQuota: 0, minUnitHours: 1, version: '' }) }
async function load() { loading.value = true; try { rows.value = await fetchManagedLeaveTypes() } catch { ElMessage.error('无法加载请假类型') } finally { loading.value = false } }
async function save() { if (!form.code.trim() || !form.name.trim() || form.minUnitHours <= 0 || (form.deductBalance && form.annualQuota <= 0)) { ElMessage.warning('请填写编码、名称、正数最小单位和年度额度'); return }; saving.value = true; try { const payload = { ...(form.id ? {} : { code: form.code.trim() }), name: form.name.trim(), deductBalance: form.deductBalance, annualQuota: form.deductBalance ? form.annualQuota : null, minUnitHours: form.minUnitHours, ...(form.id ? { version: form.version } : {}) }; if (form.id) await updateLeaveType(form.id, payload); else await createLeaveType(payload); ElMessage.success('请假类型已保存'); dialog.value = false; await load() } catch { ElMessage.error('保存失败，请刷新后重试') } finally { saving.value = false } }
async function disable(row: ManagedLeaveType) { try { await ElMessageBox.confirm(`停用“${row.name}”后不能发起新的此类请假。`, '确认停用', { type: 'warning' }); await disableLeaveType(row.id, row.version); ElMessage.success('请假类型已停用'); await load() } catch { /* cancellation and request errors leave current data intact */ } }
function deductionChanged() { if (!form.deductBalance) form.annualQuota = 0 }
onMounted(load)
</script>
<template>
  <PageFrame title="请假类型" description="维护扣减余额、年度额度和最小申请单位规则。">
    <template #actions><el-button @click="load">刷新</el-button><el-button type="primary" @click="reset(); dialog = true">新增类型</el-button></template>
    <el-table v-loading="loading" :data="rows" class="data-table">
      <el-table-column prop="code" label="编码" width="150"/><el-table-column prop="name" label="名称" min-width="180"/>
      <el-table-column label="扣减余额" width="110"><template #default="{ row }">{{ row.deductBalance ? '是' : '否' }}</template></el-table-column>
      <el-table-column prop="annualQuota" label="年度额度(小时)" width="140"/><el-table-column prop="minUnitHours" label="最小单位(小时)" width="140"/><el-table-column prop="status" label="状态" width="110"/>
      <el-table-column label="操作" width="160"><template #default="{ row }"><el-button text @click="reset(row); dialog = true">编辑</el-button><el-button v-if="row.status === 'ACTIVE'" text type="danger" @click="disable(row)">停用</el-button></template></el-table-column>
    </el-table>
    <EmptyState v-if="!loading && rows.length === 0" title="暂无请假类型" description="新增一个类型后即可配置请假申请规则。"/>
    <el-dialog v-model="dialog" :title="form.id ? '编辑请假类型' : '新增请假类型'" width="520"><el-form label-position="top"><el-form-item label="编码"><el-input v-model="form.code" :disabled="Boolean(form.id)"/></el-form-item><el-form-item label="名称"><el-input v-model="form.name"/></el-form-item><el-form-item label="扣减余额"><el-switch v-model="form.deductBalance" @change="deductionChanged"/></el-form-item><el-form-item label="年度额度（小时）"><el-input-number v-model="form.annualQuota" :disabled="!form.deductBalance" :min="0" :step="1"/></el-form-item><el-form-item label="最小申请单位（小时）"><el-input-number v-model="form.minUnitHours" :min="0.25" :step="0.25"/></el-form-item></el-form><template #footer><el-button @click="dialog = false">取消</el-button><el-button type="primary" :loading="saving" @click="save">保存</el-button></template></el-dialog>
  </PageFrame>
</template>

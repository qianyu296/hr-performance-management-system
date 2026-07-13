<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Edit, Refresh, Search, View } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import PageFrame from '@/components/common/PageFrame.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import { adjustLeaveBalance, fetchLeaveBalanceChanges, fetchLeaveBalances, type LeaveBalance, type LeaveBalanceChange } from '@/api/leave'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const loading = ref(false)
const saving = ref(false)
const balances = ref<LeaveBalance[]>([])
const changes = ref<LeaveBalanceChange[]>([])
const targetEmployeeId = ref('')
const adjustmentVisible = ref(false)
const historyVisible = ref(false)
const selectedBalance = ref<LeaveBalance | null>(null)
const adjustment = reactive({ direction: 'INCREASE' as 'INCREASE' | 'DECREASE', deltaHours: 0, reason: '' })

function formatDateTime(value: string) {
  return new Intl.DateTimeFormat('zh-CN', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value))
}

async function load() {
  loading.value = true
  try {
    balances.value = await fetchLeaveBalances(targetEmployeeId.value.trim() || undefined)
  } catch {
    balances.value = []
    ElMessage.error(targetEmployeeId.value ? '无法加载该员工的余额' : '无法加载个人余额，请使用关联员工账号登录')
  } finally {
    loading.value = false
  }
}

function openAdjust(balance: LeaveBalance) {
  selectedBalance.value = balance
  Object.assign(adjustment, { direction: 'INCREASE', deltaHours: 0, reason: '' })
  adjustmentVisible.value = true
}

async function saveAdjustment() {
  if (!selectedBalance.value || adjustment.deltaHours <= 0 || !adjustment.reason.trim()) {
    ElMessage.warning('请填写调整方向、时长和原因')
    return
  }
  saving.value = true
  try {
    await adjustLeaveBalance(selectedBalance.value.id, { ...adjustment, reason: adjustment.reason.trim(), version: selectedBalance.value.version })
    ElMessage.success('余额已调整并记录流水')
    adjustmentVisible.value = false
    await load()
  } catch {
    ElMessage.error('调整失败，请刷新后重试')
  } finally {
    saving.value = false
  }
}

async function openHistory(balance: LeaveBalance) {
  selectedBalance.value = balance
  historyVisible.value = true
  try {
    changes.value = await fetchLeaveBalanceChanges(balance.id)
  } catch {
    changes.value = []
    ElMessage.error('无法加载余额流水')
  }
}

onMounted(load)
</script>

<template>
  <PageFrame title="假期余额" description="查看假期与调休余额。余额调整必须记录方向、原因和操作流水。">
    <template #actions><el-button :icon="Refresh" @click="load">刷新</el-button></template>
    <template v-if="authStore.can('attendance:balance:adjust')" #filters>
      <el-input v-model="targetEmployeeId" inputmode="numeric" placeholder="员工 ID，留空查看本人" />
      <el-button :icon="Search" @click="load">查询</el-button>
    </template>

    <el-table v-loading="loading" :data="balances" class="data-table">
      <el-table-column prop="balanceType" label="余额类型" min-width="150" />
      <el-table-column prop="balanceYear" label="年度" width="100" />
      <el-table-column prop="availableHours" label="可用时长(小时)" width="150" />
      <el-table-column prop="frozenHours" label="冻结时长(小时)" width="150" />
      <el-table-column v-if="authStore.can('attendance:balance:adjust')" label="操作" width="130" fixed="right">
        <template #default="{ row }">
          <el-tooltip content="查看余额流水"><el-button text :icon="View" aria-label="查看余额流水" @click="openHistory(row)" /></el-tooltip>
          <el-tooltip content="调整余额"><el-button text :icon="Edit" aria-label="调整余额" @click="openAdjust(row)" /></el-tooltip>
        </template>
      </el-table-column>
    </el-table>
    <EmptyState v-if="!loading && balances.length === 0" title="暂无余额记录" description="管理员可输入员工 ID 查询；员工余额由 HR 配置或审批通过后产生。" />

    <el-dialog v-model="adjustmentVisible" title="调整假期余额" width="440px">
      <el-form label-position="top">
        <el-form-item label="调整方向"><el-radio-group v-model="adjustment.direction"><el-radio-button label="INCREASE">增加</el-radio-button><el-radio-button label="DECREASE">扣减</el-radio-button></el-radio-group></el-form-item>
        <el-form-item label="调整时长(小时)"><el-input-number v-model="adjustment.deltaHours" :min="0.01" :step="0.5" class="form-control-full" /></el-form-item>
        <el-form-item label="调整原因"><el-input v-model="adjustment.reason" type="textarea" :rows="3" maxlength="500" show-word-limit /></el-form-item>
      </el-form>
      <template #footer><el-button @click="adjustmentVisible = false">取消</el-button><el-button type="primary" :loading="saving" @click="saveAdjustment">确认调整</el-button></template>
    </el-dialog>

    <el-drawer v-model="historyVisible" title="余额流水" size="min(620px, 100vw)">
      <el-table :data="changes" class="data-table">
        <el-table-column label="变动" width="100"><template #default="{ row }"><el-tag :type="row.deltaHours >= 0 ? 'success' : 'danger'">{{ row.deltaHours >= 0 ? '+' : '' }}{{ row.deltaHours }}</el-tag></template></el-table-column>
        <el-table-column label="变动后" prop="afterHours" width="100" />
        <el-table-column label="来源" prop="sourceType" width="150" />
        <el-table-column label="原因" prop="reason" min-width="180" />
        <el-table-column label="时间" min-width="160"><template #default="{ row }">{{ formatDateTime(row.createdTime) }}</template></el-table-column>
      </el-table>
    </el-drawer>
  </PageFrame>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { Delete, Plus, Refresh, Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import PageFrame from '@/components/common/PageFrame.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import { createWorkCalendar, fetchWorkCalendar, updateWorkCalendar, type WorkCalendarPayload } from '@/api/leave'

type CalendarDayDraft = { workDate: string; workday: boolean; workHours: number; holidayName: string }

const year = ref(new Date().getFullYear())
const loading = ref(false)
const saving = ref(false)
const loaded = ref(false)
const form = reactive({
  id: '',
  name: '',
  timeZone: 'UTC',
  status: 'ACTIVE' as 'ACTIVE' | 'INACTIVE',
  version: '0',
  days: [] as CalendarDayDraft[],
})
const editing = computed(() => Boolean(form.id))
const specialDayCount = computed(() => form.days.length)

function resetForm() {
  Object.assign(form, { id: '', name: `${year.value} 工作日历`, timeZone: 'UTC', status: 'ACTIVE', version: '0', days: [] })
}

function isDefaultDay(day: CalendarDayDraft) {
  if (!day.workDate) return false
  const weekday = new Date(`${day.workDate}T00:00:00`).getDay()
  const hasHolidayName = day.holidayName.trim().length > 0
  if (hasHolidayName) return false
  if (weekday === 0 || weekday === 6) return !day.workday && day.workHours === 0
  return day.workday && day.workHours === 8
}

function normalizeDays(days: CalendarDayDraft[]) {
  return [...days]
    .filter((day) => !isDefaultDay(day))
    .sort((left, right) => left.workDate.localeCompare(right.workDate))
}

async function load() {
  loading.value = true
  loaded.value = true
  try {
    const calendar = await fetchWorkCalendar(year.value)
    Object.assign(form, {
      id: calendar.id,
      name: calendar.name,
      timeZone: calendar.timeZone,
      status: calendar.status,
      version: calendar.version,
      days: normalizeDays(
        calendar.days.map((day) => ({
          workDate: day.workDate,
          workday: day.workday,
          workHours: day.workHours,
          holidayName: day.holidayName ?? '',
        }))
      ),
    })
  } catch (error: any) {
    if (error?.response?.status === 404) resetForm()
    else ElMessage.error('无法加载工作日历')
  } finally {
    loading.value = false
  }
}

function addDay() {
  form.days.push({ workDate: '', workday: false, workHours: 0, holidayName: '' })
}

function removeDay(index: number) {
  form.days.splice(index, 1)
}

function updateDayKind(day: CalendarDayDraft) {
  if (!day.workday) day.workHours = 0
  else if (day.workHours <= 0) day.workHours = 8
}

function payload(): WorkCalendarPayload | null {
  if (!form.name.trim() || !form.timeZone.trim()) {
    ElMessage.warning('请填写日历名称和时区')
    return null
  }
  const normalizedDays = normalizeDays(form.days)
  const dates = new Set<string>()
  for (const day of normalizedDays) {
    if (!day.workDate || dates.has(day.workDate) || (day.workday && day.workHours <= 0) || (!day.workday && day.workHours !== 0)) {
      ElMessage.warning('请检查日期、重复项和工作时长')
      return null
    }
    dates.add(day.workDate)
  }
  form.days = normalizedDays
  return {
    ...(editing.value ? {} : { calendarYear: year.value }),
    name: form.name.trim(),
    timeZone: form.timeZone.trim(),
    status: form.status,
    days: normalizedDays.map((day) => ({
      workDate: day.workDate,
      workday: day.workday,
      workHours: day.workHours,
      ...(day.holidayName.trim() ? { holidayName: day.holidayName.trim() } : {}),
    })),
    ...(editing.value ? { version: form.version } : {}),
  }
}

async function save() {
  const value = payload()
  if (!value) return
  saving.value = true
  try {
    if (editing.value) await updateWorkCalendar(form.id, value)
    else await createWorkCalendar(value)
    ElMessage.success('工作日历已保存')
    await load()
  } catch {
    ElMessage.error('保存失败，请刷新后重试')
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<template>
  <PageFrame title="工作日历" description="维护工作日、节假日和调休工作日。请假时长由服务端按本日历计算。">
    <template #actions>
      <el-button :icon="Refresh" @click="load">刷新</el-button>
      <el-button type="primary" :loading="saving" @click="save">保存日历</el-button>
    </template>
    <template #filters>
      <el-input-number v-model="year" :min="2000" :max="2100" controls-position="right" />
      <el-button :icon="Search" @click="load">查询年度</el-button>
    </template>

    <div v-loading="loading" class="calendar-editor">
      <template v-if="loaded">
        <el-form label-position="top">
          <div class="form-grid">
            <el-form-item label="日历名称"><el-input v-model="form.name" /></el-form-item>
            <el-form-item label="时区"><el-input v-model="form.timeZone" placeholder="例如 UTC 或 Asia/Shanghai" /></el-form-item>
            <el-form-item label="状态"><el-radio-group v-model="form.status"><el-radio-button label="ACTIVE">启用</el-radio-button><el-radio-button label="INACTIVE">停用</el-radio-button></el-radio-group></el-form-item>
          </div>
        </el-form>
        <div class="calendar-days-heading">
          <div>
            <h2>特殊日期</h2>
            <p class="calendar-days-note">这里只维护偏离默认规则的日期。未列出的周一至周五默认按 8 小时工作日计算，周末默认不计入工时。</p>
          </div>
          <div class="calendar-days-actions">
            <span class="calendar-days-count">共 {{ specialDayCount }} 项</span>
            <el-tooltip content="新增日期"><el-button circle :icon="Plus" aria-label="新增日期" @click="addDay" /></el-tooltip>
          </div>
        </div>
        <el-table :data="form.days" class="data-table">
          <el-table-column label="日期" min-width="160"><template #default="{ row }"><el-date-picker v-model="row.workDate" type="date" value-format="YYYY-MM-DD" /></template></el-table-column>
          <el-table-column label="类型" width="160"><template #default="{ row }"><el-switch v-model="row.workday" active-text="工作日" inactive-text="非工作日" @change="updateDayKind(row)" /></template></el-table-column>
          <el-table-column label="工作时长" width="160"><template #default="{ row }"><el-input-number v-model="row.workHours" :disabled="!row.workday" :min="0" :max="24" :step="0.5" /></template></el-table-column>
          <el-table-column label="节假日/调休说明" min-width="220"><template #default="{ row }"><el-input v-model="row.holidayName" placeholder="可选" /></template></el-table-column>
          <el-table-column width="56"><template #default="{ $index }"><el-tooltip content="删除日期"><el-button circle text type="danger" :icon="Delete" aria-label="删除日期" @click="removeDay($index)" /></el-tooltip></template></el-table-column>
        </el-table>
        <EmptyState v-if="form.days.length === 0" title="未配置特殊日期" description="未列出的周一至周五按 8 小时工作日计算，周末默认不计入请假时长。" />
      </template>
    </div>
  </PageFrame>
</template>

<style scoped>
.calendar-days-note {
  margin: 8px 0 0;
  color: var(--el-text-color-secondary);
  font-size: 13px;
  line-height: 1.6;
}

.calendar-days-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.calendar-days-count {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

@media (max-width: 768px) {
  .calendar-days-actions {
    align-self: flex-start;
  }
}
</style>

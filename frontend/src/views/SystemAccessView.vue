<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, type CheckboxValueType } from 'element-plus'
import { Edit, Refresh } from '@element-plus/icons-vue'
import PageFrame from '@/components/common/PageFrame.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import { fetchSystemRoles, fetchSystemUsers, replaceUserRoles, type SystemRole, type SystemUser } from '@/api/system'

const loading = ref(false)
const saving = ref(false)
const roles = ref<SystemRole[]>([])
const users = ref<SystemUser[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)
const dialogVisible = ref(false)
const selectedUser = ref<SystemUser | null>(null)
const selectedRoleIds = ref<string[]>([])

const roleNames = computed(() => new Map(roles.value.map((role) => [role.id, role.name])))

async function loadData() {
  loading.value = true
  try {
    const [roleList, userPage] = await Promise.all([fetchSystemRoles(), fetchSystemUsers(page.value, pageSize.value)])
    roles.value = roleList
    users.value = userPage.records
    total.value = userPage.total
  } catch {
    ElMessage.error('无法加载用户权限数据')
  } finally {
    loading.value = false
  }
}

function openRoleDialog(user: SystemUser) {
  selectedUser.value = user
  selectedRoleIds.value = [...user.roleIds]
  dialogVisible.value = true
}

function updateSelectedRoles(value: CheckboxValueType[]) {
  selectedRoleIds.value = value.map(String)
}

async function saveRoles() {
  if (!selectedUser.value) return
  saving.value = true
  try {
    await replaceUserRoles(selectedUser.value.id, selectedRoleIds.value, selectedUser.value.version)
    ElMessage.success('用户角色已更新，旧会话已失效')
    dialogVisible.value = false
    await loadData()
  } catch {
    ElMessage.error('角色更新失败，请刷新后重试')
  } finally {
    saving.value = false
  }
}

async function changePage(nextPage: number) {
  page.value = nextPage
  await loadData()
}

onMounted(loadData)
</script>

<template>
  <PageFrame title="系统管理" description="管理用户角色。保存后，被调整用户需要重新登录。">
    <template #actions>
      <el-button :icon="Refresh" @click="loadData">刷新</el-button>
    </template>

    <el-table v-loading="loading" :data="users" class="data-table">
      <el-table-column prop="username" label="用户名" min-width="180" />
      <el-table-column label="关联员工" min-width="140">
        <template #default="{ row }">{{ row.employeeId ?? '-' }}</template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="120">
        <template #default="{ row }"><el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'">{{ row.status === 'ACTIVE' ? '启用' : '停用' }}</el-tag></template>
      </el-table-column>
      <el-table-column label="角色" min-width="260">
        <template #default="{ row }">
          <el-space wrap>
            <el-tag v-for="roleId in row.roleIds" :key="roleId" effect="plain">{{ roleNames.get(roleId) ?? roleId }}</el-tag>
            <span v-if="row.roleIds.length === 0">未分配</span>
          </el-space>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button text type="primary" :icon="Edit" @click="openRoleDialog(row)">调整角色</el-button>
        </template>
      </el-table-column>
    </el-table>
    <EmptyState v-if="!loading && users.length === 0" title="暂无账号" description="当前没有可管理的系统账号。" />
    <el-pagination v-if="total > pageSize" v-model:current-page="page" :page-size="pageSize" :total="total" layout="prev, pager, next" @current-change="changePage" />

    <el-dialog v-model="dialogVisible" title="调整用户角色" width="520px">
      <p v-if="selectedUser">{{ selectedUser.username }}</p>
      <el-checkbox-group :model-value="selectedRoleIds" @update:model-value="updateSelectedRoles">
        <el-space direction="vertical" alignment="start">
          <el-checkbox v-for="role in roles" :key="role.id" :label="role.id">{{ role.name }}</el-checkbox>
        </el-space>
      </el-checkbox-group>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveRoles">保存</el-button>
      </template>
    </el-dialog>
  </PageFrame>
</template>

<script setup lang="ts">
import { isAxiosError } from 'axios'
import { computed, onMounted, reactive, ref } from 'vue'
import { Edit, Plus, Refresh } from '@element-plus/icons-vue'
import { ElMessage, type CheckboxValueType } from 'element-plus'
import PageFrame from '@/components/common/PageFrame.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import {
  createSystemRole,
  fetchSystemDepartments,
  fetchSystemMenus,
  fetchSystemRole,
  fetchSystemRoles,
  fetchSystemUsers,
  replaceUserRoles,
  updateSystemRole,
  type CreateSystemRolePayload,
  type SystemMenu,
  type SystemRole,
  type SystemRoleDetail,
  type SystemUser,
  type UpdateSystemRolePayload,
} from '@/api/system'
import type { DepartmentNode } from '@/types/organization'

const loading = ref(false)
const savingUserRoles = ref(false)
const savingRole = ref(false)
const activeTab = ref('roles')
const roles = ref<SystemRole[]>([])
const menus = ref<SystemMenu[]>([])
const departments = ref<DepartmentNode[]>([])
const roleDetails = ref<Record<string, SystemRoleDetail>>({})
const users = ref<SystemUser[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)
const userDialogVisible = ref(false)
const roleDialogVisible = ref(false)
const selectedUser = ref<SystemUser | null>(null)
const selectedRoleIds = ref<string[]>([])
const roleForm = reactive({
  id: '',
  code: '',
  name: '',
  status: 'ACTIVE',
  dataScopeType: 'SELF',
  menuIds: [] as string[],
  departmentIds: [] as string[],
  version: '0',
})

const roleNames = computed(() => new Map(roles.value.map((role) => [role.id, role.name])))
const menuNames = computed(() => new Map(menus.value.map((menu) => [menu.id, menu.name])))
const isEditingRole = computed(() => Boolean(roleForm.id))
const dataScopeOptions = [
  { value: 'SELF', label: '仅本人' },
  { value: 'DIRECT', label: '直属下级' },
  { value: 'DEPT', label: '本部门' },
  { value: 'DEPT_TREE', label: '部门树' },
  { value: 'ALL', label: '全部数据' },
  { value: 'CUSTOM', label: '自定义部门' },
]
const visibleMenus = computed(() => menus.value.filter((menu) => menu.permissionCode && menu.status === 'ACTIVE'))

function dataScopeLabel(value: string) {
  return dataScopeOptions.find((item) => item.value === value)?.label ?? value
}

function rolePermissionCount(roleId: string) {
  return roleDetails.value[roleId]?.menuIds.length ?? 0
}

function roleScopeLabel(roleId: string) {
  return dataScopeLabel(roleDetails.value[roleId]?.dataScopeType ?? 'SELF')
}

function roleDepartmentSummary(roleId: string) {
  const detail = roleDetails.value[roleId]
  if (!detail || detail.dataScopeType !== 'CUSTOM') return '-'
  return detail.departmentIds.length ? `已选 ${detail.departmentIds.length} 个部门` : '未选择部门'
}

function resetRoleForm() {
  Object.assign(roleForm, {
    id: '',
    code: '',
    name: '',
    status: 'ACTIVE',
    dataScopeType: 'SELF',
    menuIds: [],
    departmentIds: [],
    version: '0',
  })
}

async function loadBaseData() {
  const [roleList, menuList, departmentTree] = await Promise.all([
    fetchSystemRoles(),
    fetchSystemMenus(),
    fetchSystemDepartments(),
  ])
  roles.value = roleList
  menus.value = menuList
  departments.value = departmentTree
  const details = await Promise.all(roleList.map((role) => fetchSystemRole(role.id)))
  roleDetails.value = Object.fromEntries(details.map((detail) => [detail.id, detail]))
}

async function loadUsers() {
  const userPage = await fetchSystemUsers(page.value, pageSize.value)
  users.value = userPage.records
  total.value = userPage.total
}

async function loadData() {
  loading.value = true
  try {
    await Promise.all([loadBaseData(), loadUsers()])
  } catch {
    ElMessage.error('无法加载系统设置数据')
  } finally {
    loading.value = false
  }
}

function openUserRoleDialog(user: SystemUser) {
  selectedUser.value = user
  selectedRoleIds.value = [...user.roleIds]
  userDialogVisible.value = true
}

function updateSelectedRoles(value: CheckboxValueType[]) {
  selectedRoleIds.value = value.map(String)
}

function extractErrorMessage(error: unknown, fallback: string) {
  if (isAxiosError(error)) {
    const message = (error.response?.data as { message?: string } | undefined)?.message
    if (message) return message
  }
  return fallback
}

function isAccessChangedError(error: unknown) {
  return isAxiosError(error) && [401, 403].includes(error.response?.status ?? 0)
}

async function saveUserRoles() {
  if (!selectedUser.value) return
  savingUserRoles.value = true
  try {
    await replaceUserRoles(selectedUser.value.id, selectedRoleIds.value, selectedUser.value.version)
    ElMessage.success('用户角色已更新，旧会话已失效')
    userDialogVisible.value = false
    try {
      await loadUsers()
    } catch (error) {
      if (isAccessChangedError(error)) {
        ElMessage.warning('用户角色已更新，但当前账号权限已变化，请重新进入系统设置页面')
        return
      }
      ElMessage.warning('用户角色已更新，但列表刷新失败，请手动刷新页面')
    }
  } catch (error) {
    ElMessage.error(extractErrorMessage(error, '角色更新失败，请刷新后重试'))
  } finally {
    savingUserRoles.value = false
  }
}

function openCreateRoleDialog() {
  resetRoleForm()
  roleDialogVisible.value = true
}

async function openEditRoleDialog(role: SystemRole) {
  try {
    const detail = await fetchSystemRole(role.id)
    const activeMenuIds = new Set(menus.value.filter((menu) => menu.status === 'ACTIVE').map((menu) => menu.id))
    Object.assign(roleForm, {
      ...detail,
      menuIds: detail.menuIds.filter((menuId) => activeMenuIds.has(menuId)),
    })
    roleDialogVisible.value = true
  } catch {
    ElMessage.error('无法加载角色详情')
  }
}

function rolePayload(): CreateSystemRolePayload | UpdateSystemRolePayload | null {
  if (!roleForm.code.trim() && !isEditingRole.value) {
    ElMessage.warning('请填写角色编码')
    return null
  }
  if (!roleForm.name.trim()) {
    ElMessage.warning('请填写角色名称')
    return null
  }
  if (roleForm.menuIds.length === 0) {
    ElMessage.warning('请至少选择一个功能权限')
    return null
  }
  if (roleForm.dataScopeType === 'CUSTOM' && roleForm.departmentIds.length === 0) {
    ElMessage.warning('自定义部门范围至少需要选择一个部门')
    return null
  }
  const activeMenuIds = new Set(menus.value.filter((menu) => menu.status === 'ACTIVE').map((menu) => menu.id))
  const selectedMenuIds = roleForm.menuIds.filter((menuId) => activeMenuIds.has(menuId))
  if (selectedMenuIds.length === 0) {
    ElMessage.warning('请至少选择一个启用中的功能权限')
    return null
  }
  const base = {
    name: roleForm.name.trim(),
    status: roleForm.status,
    dataScopeType: roleForm.dataScopeType,
    menuIds: selectedMenuIds,
    departmentIds: roleForm.dataScopeType === 'CUSTOM' ? [...roleForm.departmentIds] : [],
  }
  if (isEditingRole.value) {
    return {
      ...base,
      version: roleForm.version,
    }
  }
  return {
    ...base,
    code: roleForm.code.trim().toUpperCase(),
  }
}

async function saveRole() {
  const payload = rolePayload()
  if (!payload) return
  savingRole.value = true
  try {
    if (isEditingRole.value) {
      const updated = await updateSystemRole(roleForm.id, payload as UpdateSystemRolePayload)
      roleDetails.value[updated.id] = updated
      ElMessage.success('角色已更新')
    } else {
      const created = await createSystemRole(payload as CreateSystemRolePayload)
      roleDetails.value[created.id] = created
      ElMessage.success('角色已创建')
    }
    roleDialogVisible.value = false
  } catch (error) {
    ElMessage.error(extractErrorMessage(error, '角色保存失败，请检查编码、权限和数据范围配置'))
    return
  } finally {
    savingRole.value = false
  }

  try {
    await loadBaseData()
    await loadUsers()
  } catch (error) {
    if (isAccessChangedError(error)) {
      ElMessage.warning('角色已保存，但当前账号权限已发生变化，请重新进入系统设置页面')
      return
    }
    ElMessage.warning('角色已保存，但系统设置列表刷新失败，请手动刷新页面')
  }
}

async function changePage(nextPage: number) {
  page.value = nextPage
  await loadUsers()
}

onMounted(loadData)
</script>

<template>
  <PageFrame title="系统设置" description="管理角色、功能权限和用户授权。角色变更会在下一次请求时即时生效。">
    <template #actions>
      <el-button :icon="Refresh" @click="loadData">刷新</el-button>
      <el-button v-if="activeTab === 'roles'" type="primary" :icon="Plus" @click="openCreateRoleDialog">新建角色</el-button>
    </template>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="角色管理" name="roles">
        <el-table v-loading="loading" :data="roles" class="data-table">
          <el-table-column prop="code" label="角色编码" min-width="180" />
          <el-table-column prop="name" label="角色名称" min-width="180" />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'">{{ row.status === 'ACTIVE' ? '启用' : '停用' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="功能权限" width="120">
            <template #default="{ row }">{{ rolePermissionCount(row.id) }} 项</template>
          </el-table-column>
          <el-table-column label="数据范围" min-width="150">
            <template #default="{ row }">{{ roleScopeLabel(row.id) }}</template>
          </el-table-column>
          <el-table-column label="部门范围" min-width="150">
            <template #default="{ row }">{{ roleDepartmentSummary(row.id) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row }">
              <el-button text type="primary" :icon="Edit" @click="openEditRoleDialog(row)">编辑角色</el-button>
            </template>
          </el-table-column>
        </el-table>
        <EmptyState v-if="!loading && roles.length === 0" title="暂无角色" description="创建角色后即可为用户授予新的功能组合。" />
      </el-tab-pane>

      <el-tab-pane label="用户授权" name="users">
        <el-table v-loading="loading" :data="users" class="data-table">
          <el-table-column prop="username" label="用户名" min-width="180" />
          <el-table-column label="关联员工" min-width="140">
            <template #default="{ row }">{{ row.employeeId ?? '-' }}</template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="120">
            <template #default="{ row }">
              <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'">{{ row.status === 'ACTIVE' ? '启用' : '停用' }}</el-tag>
            </template>
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
              <el-button text type="primary" :icon="Edit" @click="openUserRoleDialog(row)">调整角色</el-button>
            </template>
          </el-table-column>
        </el-table>
        <EmptyState v-if="!loading && users.length === 0" title="暂无账号" description="当前没有可管理的系统账号。" />
        <el-pagination
          v-if="total > pageSize"
          v-model:current-page="page"
          :page-size="pageSize"
          :total="total"
          layout="prev, pager, next"
          @current-change="changePage"
        />
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="userDialogVisible" title="调整用户角色" width="520px">
      <p v-if="selectedUser">{{ selectedUser.username }}</p>
      <el-checkbox-group :model-value="selectedRoleIds" @update:model-value="updateSelectedRoles">
        <el-space direction="vertical" alignment="start">
          <el-checkbox v-for="role in roles" :key="role.id" :label="role.id" :disabled="role.status !== 'ACTIVE'">{{ role.name }}</el-checkbox>
        </el-space>
      </el-checkbox-group>
      <template #footer>
        <el-button @click="userDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingUserRoles" @click="saveUserRoles">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="roleDialogVisible" :title="isEditingRole ? '编辑角色' : '新建角色'" width="min(900px, 94vw)">
      <el-form label-position="top" class="role-form">
        <div class="form-grid role-form-grid">
          <el-form-item label="角色编码">
            <el-input v-model="roleForm.code" :disabled="isEditingRole" placeholder="例如 HR_AUDITOR" />
          </el-form-item>
          <el-form-item label="角色名称">
            <el-input v-model="roleForm.name" />
          </el-form-item>
          <el-form-item label="状态">
            <el-radio-group v-model="roleForm.status">
              <el-radio-button label="ACTIVE">启用</el-radio-button>
              <el-radio-button label="INACTIVE">停用</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="数据范围">
            <el-select v-model="roleForm.dataScopeType">
              <el-option v-for="item in dataScopeOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
        </div>

        <el-form-item v-if="roleForm.dataScopeType === 'CUSTOM'" label="自定义部门范围">
          <el-tree-select
            v-model="roleForm.departmentIds"
            :data="departments"
            multiple
            show-checkbox
            check-strictly
            node-key="id"
            :props="{ label: 'name', children: 'children' }"
            placeholder="请选择可访问的部门"
            class="form-control-full"
          />
        </el-form-item>

        <el-form-item label="功能权限">
          <el-checkbox-group v-model="roleForm.menuIds" class="role-menu-grid">
            <el-checkbox v-for="menu in visibleMenus" :key="menu.id" :label="menu.id" class="role-menu-item">
              <div class="role-menu-name">{{ menu.name }}</div>
              <div class="role-menu-code">{{ menu.permissionCode }}</div>
            </el-checkbox>
          </el-checkbox-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="roleDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingRole" @click="saveRole">保存</el-button>
      </template>
    </el-dialog>
  </PageFrame>
</template>

<style scoped>
.role-form-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.role-menu-grid {
  display: grid;
  width: 100%;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 12px;
}

.role-menu-item {
  margin-right: 0;
  padding: 10px 12px;
  border: 1px solid var(--el-border-color);
  border-radius: 8px;
}

.role-menu-name {
  font-weight: 600;
}

.role-menu-code {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
</style>
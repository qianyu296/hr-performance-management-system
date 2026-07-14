<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { Delete, Edit, Plus, Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import PageFrame from '@/components/common/PageFrame.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import { fetchDepartmentTree } from '@/api/organization'
import { createWorkflowTemplate, fetchWorkflowTemplates, updateWorkflowTemplate, type WorkflowNodeType, type WorkflowTemplate, type WorkflowTemplatePayload } from '@/api/workflow'
import type { DepartmentNode } from '@/types/organization'

type DraftNode = { nodeType: WorkflowNodeType; userId: string; roleCode: string }
type BusinessType = WorkflowTemplate['businessType']

const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const templates = ref<WorkflowTemplate[]>([])
const departments = ref<DepartmentNode[]>([])
const form = reactive({
  id: '',
  code: '',
  name: '',
  businessType: 'LEAVE' as BusinessType,
  priority: 0,
  templateVersion: 1,
  status: 'ACTIVE' as WorkflowTemplate['status'],
  version: '0',
  departmentIds: [] as string[],
  nodes: [newNode()] as DraftNode[],
})

const isEditing = computed(() => Boolean(form.id))
const businessLabels: Record<Exclude<BusinessType, 'PERFORMANCE_APPEAL'>, string> = {
  LEAVE: '请假',
  OVERTIME: '加班',
  PERSONNEL_CHANGE: '人事异动',
}
const visibleTemplates = computed(() => templates.value.filter((template) => template.businessType !== 'PERFORMANCE_APPEAL'))
const nodeLabels: Record<WorkflowNodeType, string> = {
  SPECIFIC_USER: '指定人员',
  DIRECT_MANAGER: '直属主管',
  DEPARTMENT_LEADER: '部门负责人',
  HR: 'HR 角色',
}

function businessLabel(value: string) {
  return businessLabels[value as keyof typeof businessLabels] ?? value
}

function nodeLabel(value: string) {
  return nodeLabels[value as WorkflowNodeType] ?? value
}

function newNode(): DraftNode {
  return { nodeType: 'DIRECT_MANAGER', userId: '', roleCode: 'HR' }
}

function resetForm() {
  Object.assign(form, {
    id: '', code: '', name: '', businessType: 'LEAVE', priority: 0, templateVersion: 1,
    status: 'ACTIVE', version: '0', departmentIds: [], nodes: [newNode()],
  })
}

async function loadData() {
  loading.value = true
  try {
    const [templateList, departmentTree] = await Promise.all([fetchWorkflowTemplates(), fetchDepartmentTree()])
    templates.value = templateList
    departments.value = departmentTree
  } catch {
    ElMessage.error('无法加载流程模板')
  } finally {
    loading.value = false
  }
}

function openCreate() {
  resetForm()
  dialogVisible.value = true
}

function openEdit(template: WorkflowTemplate) {
  Object.assign(form, {
    id: template.id,
    code: template.code,
    name: template.name,
    businessType: template.businessType,
    priority: template.priority,
    templateVersion: Number(template.templateVersion),
    status: template.status,
    version: template.version,
    departmentIds: [...template.departmentIds],
    nodes: template.nodes.map((node) => ({
      nodeType: node.nodeType,
      userId: node.nodeType === 'SPECIFIC_USER' ? String(node.approverRule.userId ?? '') : '',
      roleCode: node.nodeType === 'HR' ? String(node.approverRule.roleCode ?? 'HR') : 'HR',
    })),
  })
  dialogVisible.value = true
}

function addNode() {
  form.nodes.push(newNode())
}

function removeNode(index: number) {
  if (form.nodes.length === 1) return
  form.nodes.splice(index, 1)
}

function toPayload(): WorkflowTemplatePayload | null {
  if (!form.code.trim() || !form.name.trim()) {
    ElMessage.warning('请填写模板编码和名称')
    return null
  }
  const nodes = form.nodes.map((node, index) => {
    if (node.nodeType === 'SPECIFIC_USER' && !/^\d+$/.test(node.userId)) return null
    const approverRule: Record<string, unknown> = { type: node.nodeType }
    if (node.nodeType === 'SPECIFIC_USER') approverRule.userId = Number(node.userId)
    if (node.nodeType === 'HR') approverRule.roleCode = node.roleCode.trim() || 'HR'
    return { nodeNo: index + 1, nodeType: node.nodeType, approverRule }
  })
  if (nodes.some((node) => node === null)) {
    ElMessage.warning('指定人员节点需要有效的用户 ID')
    return null
  }
  return {
    ...(isEditing.value ? {} : { code: form.code.trim(), templateVersion: form.templateVersion }),
    name: form.name.trim(), businessType: form.businessType, priority: form.priority, status: form.status,
    departmentIds: form.departmentIds, nodes: nodes as NonNullable<typeof nodes[number]>[],
    ...(isEditing.value ? { version: form.version } : {}),
  }
}

async function save() {
  const payload = toPayload()
  if (!payload) return
  saving.value = true
  try {
    if (isEditing.value) await updateWorkflowTemplate(form.id, payload)
    else await createWorkflowTemplate(payload)
    ElMessage.success(isEditing.value ? '流程模板已更新' : '流程模板已创建')
    dialogVisible.value = false
    await loadData()
  } catch {
    ElMessage.error('保存失败，请刷新后重试')
  } finally {
    saving.value = false
  }
}

onMounted(loadData)
</script>

<template>
  <PageFrame title="流程模板" description="配置业务审批链。提交后会冻结节点和已解析审批人，后续修改只影响新提交的单据。">
    <template #actions>
      <el-button :icon="Refresh" @click="loadData">刷新</el-button>
      <el-button type="primary" :icon="Plus" @click="openCreate">新建模板</el-button>
    </template>

      <el-table v-loading="loading" :data="visibleTemplates" class="data-table">
      <el-table-column prop="code" label="编码" min-width="130" />
      <el-table-column prop="name" label="名称" min-width="190" />
      <el-table-column label="业务" width="120"><template #default="{ row }">{{ businessLabel(row.businessType) }}</template></el-table-column>
      <el-table-column label="适用范围" min-width="160"><template #default="{ row }">{{ row.departmentIds.length ? `部门 ${row.departmentIds.join('、')}` : '全公司' }}</template></el-table-column>
      <el-table-column label="节点" min-width="260"><template #default="{ row }"><el-tag v-for="node in row.nodes" :key="node.nodeNo" effect="plain" class="workflow-node-tag">{{ node.nodeNo }}. {{ nodeLabel(node.nodeType) }}</el-tag></template></el-table-column>
      <el-table-column prop="priority" label="优先级" width="90" />
      <el-table-column label="状态" width="100"><template #default="{ row }"><el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'">{{ row.status === 'ACTIVE' ? '启用' : '停用' }}</el-tag></template></el-table-column>
      <el-table-column label="操作" width="76" fixed="right"><template #default="{ row }"><el-tooltip content="编辑模板"><el-button text :icon="Edit" aria-label="编辑模板" @click="openEdit(row)" /></el-tooltip></template></el-table-column>
    </el-table>
    <EmptyState v-if="!loading && visibleTemplates.length === 0" title="暂无流程模板" description="创建模板后，请假和后续业务将按范围、优先级和版本选择审批链。" />

    <el-dialog v-model="dialogVisible" :title="isEditing ? '编辑流程模板' : '新建流程模板'" width="min(840px, 94vw)" destroy-on-close>
      <el-form label-position="top" class="workflow-template-form">
        <div class="form-grid">
          <el-form-item label="模板编码"><el-input v-model="form.code" :disabled="isEditing" placeholder="例如 LEAVE_STANDARD" /></el-form-item>
          <el-form-item label="模板名称"><el-input v-model="form.name" /></el-form-item>
          <el-form-item label="业务类型"><el-select v-model="form.businessType" :disabled="isEditing"><el-option v-for="(label, value) in businessLabels" :key="value" :label="label" :value="value" /></el-select></el-form-item>
          <el-form-item label="优先级"><el-input-number v-model="form.priority" :min="0" :max="9999" /></el-form-item>
          <el-form-item v-if="!isEditing" label="模板版本"><el-input-number v-model="form.templateVersion" :min="1" /></el-form-item>
          <el-form-item label="状态"><el-radio-group v-model="form.status"><el-radio-button label="ACTIVE">启用</el-radio-button><el-radio-button label="INACTIVE">停用</el-radio-button></el-radio-group></el-form-item>
        </div>
        <el-form-item label="适用部门"><el-tree-select v-model="form.departmentIds" :data="departments" multiple show-checkbox check-strictly node-key="id" :props="{ label: 'name', children: 'children' }" placeholder="留空表示全公司" class="form-control-full" /></el-form-item>
        <div class="workflow-node-editor">
          <div class="workflow-node-heading"><strong>审批节点</strong><el-tooltip content="新增节点"><el-button circle :icon="Plus" aria-label="新增节点" @click="addNode" /></el-tooltip></div>
          <div v-for="(node, index) in form.nodes" :key="index" class="workflow-node-row">
            <span class="workflow-node-number">{{ index + 1 }}</span>
            <el-select v-model="node.nodeType" aria-label="审批人类型"><el-option v-for="(label, value) in nodeLabels" :key="value" :label="label" :value="value" /></el-select>
            <el-input v-if="node.nodeType === 'SPECIFIC_USER'" v-model="node.userId" inputmode="numeric" placeholder="审批用户 ID" />
            <el-input v-else-if="node.nodeType === 'HR'" v-model="node.roleCode" placeholder="HR 角色编码" />
            <span v-else class="workflow-node-hint">{{ node.nodeType === 'DIRECT_MANAGER' ? '提交人直属主管' : '提交人所在部门负责人' }}</span>
            <el-tooltip content="删除节点"><el-button :disabled="form.nodes.length === 1" circle text type="danger" :icon="Delete" aria-label="删除节点" @click="removeNode(index)" /></el-tooltip>
          </div>
        </div>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" :loading="saving" @click="save">保存</el-button></template>
    </el-dialog>
  </PageFrame>
</template>

import { createRouter, createWebHistory } from 'vue-router'
import DashboardView from '@/views/DashboardView.vue'
import LeaveManagementView from '@/views/LeaveManagementView.vue'
import OvertimeManagementView from '@/views/OvertimeManagementView.vue'
import WorkCalendarView from '@/views/WorkCalendarView.vue'
import LeaveBalancesView from '@/views/LeaveBalancesView.vue'
import LeaveTypeManagementView from '@/views/LeaveTypeManagementView.vue'
import AttendanceSummaryView from '@/views/AttendanceSummaryView.vue'
import LoginView from '@/views/LoginView.vue'
import WorkflowTasksView from '@/views/WorkflowTasksView.vue'
import WorkflowTemplatesView from '@/views/WorkflowTemplatesView.vue'
import OrganizationEmployeesView from '@/views/OrganizationEmployeesView.vue'
import OrganizationDepartmentsView from '@/views/OrganizationDepartmentsView.vue'
import PersonnelChangesView from '@/views/PersonnelChangesView.vue'
import PersonnelChangeDetailView from '@/views/PersonnelChangeDetailView.vue'
import SystemAccessView from '@/views/SystemAccessView.vue'
import ReportsOverviewView from '@/views/ReportsOverviewView.vue'
import ChangePasswordView from '@/views/ChangePasswordView.vue'
import PeopleCenterView from '@/views/PeopleCenterView.vue'
import AttendanceCenterView from '@/views/AttendanceCenterView.vue'
import WorkflowCenterView from '@/views/WorkflowCenterView.vue'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/dashboard' },
    { path: '/login', name: 'login', component: LoginView, meta: { layout: 'blank', public: true, title: '登录' } },
    { path: '/change-password', name: 'change-password', component: ChangePasswordView, meta: { title: '修改初始密码' } },
    {
      path: '/dashboard',
      name: 'dashboard',
      component: DashboardView,
      meta: { title: '工作台', description: '待办、通知和关键工作概览' },
    },
    {
      path: '/people',
      name: 'people',
      component: PeopleCenterView,
      meta: { title: '组织人事', description: '组织架构、员工档案和人事异动统一入口', permission: ['org:read', 'personnel:read'] },
    },
    {
      path: '/attendance',
      name: 'attendance',
      component: AttendanceCenterView,
      meta: { title: '假勤中心', description: '请假、加班、余额与规则配置统一入口', permission: ['attendance:submit', 'attendance:manage', 'attendance:balance:adjust'] },
    },
    {
      path: '/workflow',
      name: 'workflow',
      component: WorkflowCenterView,
      meta: { title: '审批中心', description: '审批处理和流程模板配置统一入口', permission: ['workflow:approve', 'workflow:manage'] },
    },
    {
      path: '/org/departments',
      name: 'org-departments',
      component: OrganizationDepartmentsView,
      meta: { title: '组织架构', description: '部门层级、负责人和组织信息', permission: 'org:read' },
    },
    {
      path: '/org/employees',
      name: 'org-employees',
      component: OrganizationEmployeesView,
      meta: { title: '员工档案', description: '员工目录、任职信息和履历记录', permission: 'org:read' },
    },
    {
      path: '/personnel/changes',
      name: 'personnel-changes',
      component: PersonnelChangesView,
      meta: { title: '人事异动', description: '入转调离申请与审批进度', permission: 'personnel:read' },
    },
    {
      path: '/attendance/leave',
      name: 'attendance-leave',
      component: LeaveManagementView,
      meta: { title: '请假管理', description: '请假申请、审批状态和撤销处理', permission: 'attendance:submit' },
    },
    {
      path: '/attendance/overtime',
      name: 'attendance-overtime',
      component: OvertimeManagementView,
      meta: { title: '加班管理', description: '加班申请、补偿方式和审批状态', permission: 'attendance:submit' },
    },
    {
      path: '/attendance/leave-types',
      name: 'attendance-leave-types',
      component: LeaveTypeManagementView,
      meta: { title: '请假类型', description: '维护年度额度和最小申请单位规则', permission: 'attendance:manage' },
    },
    {
      path: '/attendance/calendar',
      name: 'attendance-calendar',
      component: WorkCalendarView,
      meta: { title: '工作日历', description: '节假日、调休和工作时长配置', permission: 'attendance:manage' },
    },
    {
      path: '/attendance/balances',
      name: 'attendance-balances',
      component: LeaveBalancesView,
      meta: { title: '假期余额', description: '个人余额、调整流水和统计口径', permission: ['attendance:submit', 'attendance:balance:adjust'] },
    },
    {
      path: '/attendance/summary',
      name: 'attendance-summary',
      component: AttendanceSummaryView,
      meta: { title: '月度假勤汇总', description: '按员工汇总请假、加班、调休和待审批', permission: 'attendance:manage' },
    },
    {
      path: '/workflow/tasks',
      name: 'workflow-tasks',
      component: WorkflowTasksView,
      meta: { title: '审批任务', description: '待办任务、审批历史和流程处理', permission: 'workflow:approve' },
    },
    {
      path: '/workflow/templates',
      name: 'workflow-templates',
      component: WorkflowTemplatesView,
      meta: { title: '流程模板', description: '审批节点、范围和优先级配置', permission: 'workflow:manage' },
    },
    {
      path: '/reports/overview',
      name: 'reports-overview',
      component: ReportsOverviewView,
      meta: { title: '数据分析', description: '人员规模和假勤指标分析', permission: 'report:read' },
    },
    {
      path: '/system/users',
      name: 'system-users',
      component: SystemAccessView,
      meta: { title: '系统设置', description: '权限分配和系统级配置', permission: 'system:manage' },
    },
    {
      path: '/personnel/changes/:id',
      name: 'personnel-change-detail',
      component: PersonnelChangeDetailView,
      meta: { title: '异动详情', description: '查看异动明细、前后差异和交接事项', permission: 'personnel:read' },
    },
    { path: '/:pathMatch(.*)*', redirect: '/dashboard' },
  ],
})

router.beforeEach(async (to) => {
  const authStore = useAuthStore()
  if (to.meta.public) {
    if (to.path === '/login' && authStore.isAuthenticated) return '/dashboard'
    return true
  }
  if (!authStore.isAuthenticated) return { path: '/login', query: { redirect: to.fullPath } }
  if (authStore.passwordChangeRequired && to.path !== '/change-password') return '/change-password'
  if (to.path === '/change-password') return true
  try {
    await authStore.loadCurrentUser()
    if (!authStore.can(Array.isArray(to.meta.permission) || typeof to.meta.permission === 'string' ? to.meta.permission : undefined)) return '/dashboard'
    return true
  } catch {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
})

export default router

import { createRouter, createWebHistory } from 'vue-router'
import DashboardView from '@/views/DashboardView.vue'
import DomainView from '@/views/DomainView.vue'
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
import { useAuthStore } from '@/stores/auth'
import { navigationItems } from './navigation'

const routedComponents: Record<string, unknown> = {
  '/org/departments': OrganizationDepartmentsView,
  '/org/employees': OrganizationEmployeesView,
  '/personnel/changes': PersonnelChangesView,
  '/attendance/leave': LeaveManagementView,
  '/attendance/leave-types': LeaveTypeManagementView,
  '/attendance/overtime': OvertimeManagementView,
  '/attendance/calendar': WorkCalendarView,
  '/attendance/balances': LeaveBalancesView,
  '/attendance/summary': AttendanceSummaryView,
  '/workflow/tasks': WorkflowTasksView,
  '/workflow/templates': WorkflowTemplatesView,
  '/system/users': SystemAccessView,
  '/reports/overview': ReportsOverviewView,
}

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
    ...navigationItems
      .filter((item) => item.path !== '/dashboard')
      .map((item) => ({
        path: item.path,
        name: item.path.replaceAll('/', '-').slice(1),
        component: routedComponents[item.path] ?? DomainView,
        meta: { title: item.title, description: item.description, permission: item.permission },
      })),
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
    if (!authStore.can(typeof to.meta.permission === 'string' ? to.meta.permission : undefined)) return '/dashboard'
    return true
  } catch {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
})

export default router

import { createRouter, createWebHistory } from 'vue-router'
import DashboardView from '@/views/DashboardView.vue'
import DomainView from '@/views/DomainView.vue'
import LeaveManagementView from '@/views/LeaveManagementView.vue'
import LoginView from '@/views/LoginView.vue'
import WorkflowTasksView from '@/views/WorkflowTasksView.vue'
import OrganizationEmployeesView from '@/views/OrganizationEmployeesView.vue'
import { useAuthStore } from '@/stores/auth'
import { navigationItems } from './navigation'

const routedComponents: Record<string, unknown> = {
  '/org/departments': OrganizationEmployeesView,
  '/org/employees': OrganizationEmployeesView,
  '/attendance/leave': LeaveManagementView,
  '/workflow/tasks': WorkflowTasksView,
}

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/dashboard' },
    { path: '/login', name: 'login', component: LoginView, meta: { layout: 'blank', public: true, title: '登录' } },
    {
      path: '/dashboard',
      name: 'dashboard',
      component: DashboardView,
      meta: { title: '工作台', description: '待办、通知和关键工作概览', permission: 'dashboard:view' },
    },
    ...navigationItems
      .filter((item) => item.path !== '/dashboard')
      .map((item) => ({
        path: item.path,
        name: item.path.replaceAll('/', '-').slice(1),
        component: routedComponents[item.path] ?? DomainView,
        meta: { title: item.title, description: item.description },
      })),
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
  try {
    await authStore.loadCurrentUser()
    return true
  } catch {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
})

export default router

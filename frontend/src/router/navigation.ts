import type { Component } from 'vue'
import { Calendar, DocumentChecked, Grid, Histogram, OfficeBuilding, Setting } from '@element-plus/icons-vue'

export interface NavigationItem {
  path: string
  title: string
  description: string
  icon: Component
  permissions?: string[]
}

export const navigationItems: NavigationItem[] = [
  { path: '/dashboard', title: '工作台', description: '待办、通知和关键工作概览', icon: Grid },
  { path: '/people', title: '组织人事', description: '组织架构、员工档案和人事异动统一入口', icon: OfficeBuilding, permissions: ['org:read', 'personnel:read'] },
  { path: '/attendance', title: '假勤中心', description: '请假、加班、余额与规则配置统一入口', icon: Calendar, permissions: ['attendance:submit', 'attendance:manage', 'attendance:balance:adjust'] },
  { path: '/workflow', title: '审批中心', description: '审批处理和流程模板配置统一入口', icon: DocumentChecked, permissions: ['workflow:approve', 'workflow:manage'] },
  { path: '/reports/overview', title: '数据分析', description: '人员规模和假勤指标分析', icon: Histogram, permissions: ['report:read'] },
  { path: '/system/users', title: '系统设置', description: '权限分配和系统级配置', icon: Setting, permissions: ['system:manage'] },
]

export function visibleNavigationItems(permissions: readonly string[]) {
  return navigationItems.filter((item) => !item.permissions || item.permissions.some((permission) => permissions.includes(permission)))
}

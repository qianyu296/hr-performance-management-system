import type { Component } from 'vue'
import { Calendar, DocumentChecked, Grid, Histogram, OfficeBuilding, Setting } from '@element-plus/icons-vue'

export interface NavigationItem {
  path: string
  title: string
  description: string
  icon?: Component
  permissions?: string[]
  children?: NavigationItem[]
}

export const navigationItems: NavigationItem[] = [
  { path: '/dashboard', title: '工作台', description: '待办、通知和关键工作概览', icon: Grid },
  {
    path: '/people',
    title: '组织人事',
    description: '组织架构、员工档案和人事异动统一入口',
    icon: OfficeBuilding,
    permissions: ['org:read', 'personnel:read', 'personnel:create', 'personnel:manage', 'personnel:approve', 'personnel:execute'],
    children: [
      { path: '/org/departments', title: '组织架构', description: '部门层级、负责人和组织信息', permissions: ['org:read'] },
      { path: '/org/employees', title: '员工档案', description: '员工目录、任职信息和履历记录', permissions: ['org:read'] },
      { path: '/personnel/changes', title: '人事异动', description: '入转调离申请与审批进度', permissions: ['personnel:read', 'personnel:create', 'personnel:manage', 'personnel:approve', 'personnel:execute'] },
    ],
  },
  {
    path: '/attendance',
    title: '假勤中心',
    description: '请假、加班、余额与规则配置统一入口',
    icon: Calendar,
    permissions: ['attendance:submit', 'attendance:manage', 'attendance:balance:adjust'],
    children: [
      { path: '/attendance/leave', title: '请假管理', description: '请假申请、审批状态和撤销处理', permissions: ['attendance:submit'] },
      { path: '/attendance/overtime', title: '加班管理', description: '加班申请、补偿方式和审批状态', permissions: ['attendance:submit'] },
      { path: '/attendance/balances', title: '假期余额', description: '个人余额、调整流水和统计口径', permissions: ['attendance:submit', 'attendance:balance:adjust'] },
      { path: '/attendance/leave-types', title: '请假类型', description: '维护年度额度和最小申请单位规则', permissions: ['attendance:manage'] },
      { path: '/attendance/calendar', title: '工作日历', description: '节假日、调休和工作时长配置', permissions: ['attendance:manage'] },
    ],
  },
  {
    path: '/workflow',
    title: '审批中心',
    description: '审批处理和流程模板配置统一入口',
    icon: DocumentChecked,
    permissions: ['workflow:approve', 'workflow:manage'],
    children: [
      { path: '/workflow/tasks', title: '审批任务', description: '待办任务、审批历史和流程处理', permissions: ['workflow:approve'] },
      { path: '/workflow/templates', title: '流程模板', description: '审批节点、范围和优先级配置', permissions: ['workflow:manage'] },
    ],
  },
  {
    path: '/reports',
    title: '数据分析',
    description: '人员规模与假勤统计分析入口',
    icon: Histogram,
    permissions: ['report:read'],
    children: [
      { path: '/reports/overview', title: '人员分析', description: '查看部门人数和组织规模分布', permissions: ['report:read'] },
      { path: '/reports/attendance', title: '假勤统计', description: '按员工汇总请假、加班、调休和待审批', permissions: ['report:read'] },
    ],
  },
  { path: '/system/users', title: '系统设置', description: '权限分配和系统级配置', icon: Setting, permissions: ['system:manage'] },
]

export function visibleNavigationItems(permissions: readonly string[]) {
  return navigationItems
    .filter((item) => {
      const hasPermission = !item.permissions || item.permissions.some((p) => permissions.includes(p))
      if (!hasPermission) return false
      if (item.children) {
        const visibleChildren = item.children.filter((child) => !child.permissions || child.permissions.some((p) => permissions.includes(p)))
        return visibleChildren.length > 0
      }
      return true
    })
    .map((item) => {
      if (!item.children) return item
      return {
        ...item,
        children: item.children.filter((child) => !child.permissions || child.permissions.some((p) => permissions.includes(p))),
      }
    })
}

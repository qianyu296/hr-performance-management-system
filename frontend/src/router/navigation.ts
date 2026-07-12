import type { Component } from 'vue'
import { Calendar, DataAnalysis, DocumentChecked, Grid, Histogram, OfficeBuilding, Operation, Setting, UserFilled } from '@element-plus/icons-vue'

export interface NavigationItem {
  path: string
  title: string
  description: string
  group: string
  icon: Component
}

export const navigationItems: NavigationItem[] = [
  { path: '/dashboard', title: '工作台', description: '待办、通知和关键工作概览', group: '工作台', icon: Grid },
  { path: '/org/departments', title: '组织架构', description: '部门层级、负责人和组织信息', group: '组织人事', icon: OfficeBuilding },
  { path: '/org/employees', title: '员工档案', description: '员工目录、任职信息和履历记录', group: '组织人事', icon: UserFilled },
  { path: '/personnel/changes', title: '人事异动', description: '入转调离申请与审批进度', group: '组织人事', icon: Operation },
  { path: '/attendance/leave', title: '请假管理', description: '请假申请、审批状态和撤销处理', group: '假勤管理', icon: Calendar },
  { path: '/attendance/balances', title: '假期余额', description: '个人余额、调整流水和统计口径', group: '假勤管理', icon: Histogram },
  { path: '/goals/cycles', title: '目标管理', description: '目标周期、目标树和进度记录', group: '目标绩效', icon: DocumentChecked },
  { path: '/performance/cycles', title: '绩效周期', description: '绩效周期、方案和任务进度', group: '目标绩效', icon: DataAnalysis },
  { path: '/workflow/tasks', title: '审批中心', description: '待办任务、审批历史和流程处理', group: '审批协同', icon: DocumentChecked },
  { path: '/reports/overview', title: '数据分析', description: '人力、假勤和绩效指标分析', group: '数据分析', icon: Histogram },
  { path: '/system/users', title: '系统管理', description: '用户、角色、菜单和审计配置', group: '系统管理', icon: Setting },
]

export const navigationGroups = Array.from(
  navigationItems.reduce((groups, item) => {
    const group = groups.get(item.group) ?? []
    group.push(item)
    groups.set(item.group, group)
    return groups
  }, new Map<string, NavigationItem[]>())
).map(([title, items]) => ({ title, items }))

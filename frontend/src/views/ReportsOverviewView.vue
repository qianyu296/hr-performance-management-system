<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import PageFrame from '@/components/common/PageFrame.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import { fetchDepartmentHeadcounts, fetchPerformanceLevelDistribution, type DepartmentHeadcount, type PerformanceLevelDistribution } from '@/api/reports'

const departments = ref<DepartmentHeadcount[]>([]); const levels = ref<PerformanceLevelDistribution[]>([]); const loading = ref(false)
const totalHeadcount = computed(() => departments.value.reduce((total, item) => total + item.headcount, 0)); const publishedCount = computed(() => levels.value.reduce((total, item) => total + item.count, 0))
async function load() { loading.value = true; try { [departments.value, levels.value] = await Promise.all([fetchDepartmentHeadcounts(), fetchPerformanceLevelDistribution()]) } catch { ElMessage.error('无法加载分析数据') } finally { loading.value = false } }
onMounted(load)
</script>
<template><PageFrame title="数据分析" description="查看组织人员规模与已发布绩效结果分布。"><template #actions><el-button @click="load">刷新</el-button></template><div class="metric-grid"><article class="metric-item"><span>在职人员</span><strong>{{ totalHeadcount }}</strong><small>按当前启用部门汇总</small></article><article class="metric-item"><span>已发布绩效结果</span><strong>{{ publishedCount }}</strong><small>仅统计员工可见结果</small></article></div><div class="report-grid" v-loading="loading"><section><div class="section-heading"><h2>部门人员规模</h2></div><el-table :data="departments" class="data-table"><el-table-column prop="departmentName" label="部门"/><el-table-column prop="headcount" label="在职人数" width="140"/></el-table><EmptyState v-if="!loading && !departments.length" title="暂无人员数据" description="创建启用部门和员工后会显示汇总。"/></section><section><div class="section-heading"><h2>绩效等级分布</h2></div><el-table :data="levels" class="data-table"><el-table-column prop="levelCode" label="等级"/><el-table-column prop="count" label="人数" width="140"/></el-table><EmptyState v-if="!loading && !levels.length" title="暂无已发布结果" description="发布绩效周期后会显示等级分布。"/></section></div></PageFrame></template>
<style scoped>.report-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:20px}.report-grid section{min-width:0}@media (max-width:900px){.report-grid{grid-template-columns:1fr}}</style>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import PageFrame from '@/components/common/PageFrame.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import { fetchDepartmentHeadcounts, type DepartmentHeadcount } from '@/api/reports'

const departments = ref<DepartmentHeadcount[]>([]); const loading = ref(false)
const totalHeadcount = computed(() => departments.value.reduce((total, item) => total + item.headcount, 0))
async function load() { loading.value = true; try { departments.value = await fetchDepartmentHeadcounts() } catch { ElMessage.error('无法加载人员分析数据') } finally { loading.value = false } }
onMounted(load)
</script>
<template><PageFrame title="数据分析" description="查看组织人员规模。"><template #actions><el-button @click="load">刷新</el-button></template><div class="metric-grid"><article class="metric-item"><span>在职人员</span><strong>{{ totalHeadcount }}</strong><small>按当前启用部门汇总</small></article></div><section v-loading="loading"><div class="section-heading"><h2>部门人员规模</h2></div><el-table :data="departments" class="data-table"><el-table-column prop="departmentName" label="部门"/><el-table-column prop="headcount" label="在职人数" width="140"/></el-table><EmptyState v-if="!loading && !departments.length" title="暂无人员数据" description="创建启用部门和员工后会显示汇总。"/></section></PageFrame></template>

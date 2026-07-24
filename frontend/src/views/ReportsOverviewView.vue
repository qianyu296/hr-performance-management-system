<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { BarChart, PieChart } from 'echarts/charts'
import { GridComponent, LegendComponent, TitleComponent, TooltipComponent } from 'echarts/components'
import * as echarts from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import type { ECharts, EChartsOption } from 'echarts'
import PageFrame from '@/components/common/PageFrame.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import { fetchDepartmentHeadcounts, type DepartmentHeadcount } from '@/api/reports'

echarts.use([BarChart, PieChart, GridComponent, LegendComponent, TitleComponent, TooltipComponent, CanvasRenderer])

const router = useRouter()
const departments = ref<DepartmentHeadcount[]>([])
const loading = ref(false)
const barEl = ref<HTMLDivElement | null>(null)
const pieEl = ref<HTMLDivElement | null>(null)
const reduceMotion = matchMedia('(prefers-reduced-motion: reduce)').matches
const palette = ['#2563eb', '#0f9f8f', '#38bdf8', '#f59e0b', '#64748b', '#8b5cf6', '#f97316', '#14b8a6']
let barChart: ECharts | null = null
let pieChart: ECharts | null = null

const rows = computed(() => [...departments.value].sort((a, b) => b.headcount - a.headcount))
const total = computed(() => rows.value.reduce((sum, item) => sum + item.headcount, 0))
const average = computed(() => rows.value.length ? Math.round(total.value / rows.value.length * 10) / 10 : 0)
const largest = computed(() => rows.value[0] ?? null)
const smallest = computed(() => rows.value.at(-1) ?? null)
const spread = computed(() => largest.value && smallest.value ? largest.value.headcount - smallest.value.headcount : 0)
const topThreeShare = computed(() => total.value
  ? rows.value.slice(0, 3).reduce((sum, item) => sum + item.headcount, 0) / total.value
  : 0)
const chartRows = computed(() => rows.value.map((item, index) => ({
  ...item,
  color: palette[index % palette.length],
  share: total.value ? item.headcount / total.value : 0,
})))
const summary = computed(() => largest.value
  ? largest.value.departmentName + '是当前人数最多的部门，占组织总人数的 '
    + percent(largest.value.headcount / total.value) + '；前三大部门合计占 ' + percent(topThreeShare.value) + '。'
  : '录入部门和员工数据后，系统将自动生成组织结构洞察。')

function percent(value: number) {
  return (value * 100).toFixed(value >= 0.1 ? 0 : 1) + '%'
}

function renderCharts() {
  if (!chartRows.value.length) return
  if (barEl.value) barChart = barChart ?? echarts.init(barEl.value)
  if (pieEl.value) pieChart = pieChart ?? echarts.init(pieEl.value)
  if (!barChart || !pieChart) return

  const tooltip = {
    backgroundColor: '#0b1f3a',
    borderColor: '#0b1f3a',
    padding: [10, 12],
    textStyle: { color: '#f8fafc', fontSize: 13 },
    extraCssText: 'border-radius:8px;box-shadow:0 12px 28px rgba(15,23,42,.18)',
  }
  const barOption: EChartsOption = {
    animation: !reduceMotion,
    animationDuration: 420,
    grid: { left: 8, right: 50, top: 10, bottom: 8, containLabel: true },
    tooltip: {
      ...tooltip,
      trigger: 'axis',
      axisPointer: { type: 'shadow', shadowStyle: { color: 'rgba(37,99,235,.06)' } },
      formatter: (params: any) => {
        const data = chartRows.value[(Array.isArray(params) ? params[0] : params).dataIndex]
        return '<strong>' + data.departmentName + '</strong><br/>在职人数　' + data.headcount
          + ' 人<br/>组织占比　' + percent(data.share)
      },
    },
    xAxis: {
      type: 'value',
      minInterval: 1,
      axisLabel: { color: '#8390a3', fontSize: 11 },
      axisLine: { show: false },
      axisTick: { show: false },
      splitLine: { lineStyle: { color: '#edf1f6', type: 'dashed' } },
    },
    yAxis: {
      type: 'category',
      inverse: true,
      data: chartRows.value.map((item) => item.departmentName),
      axisTick: { show: false },
      axisLine: { show: false },
      axisLabel: { color: '#27364b', fontSize: 12, fontWeight: 600, margin: 14 },
    },
    series: [{
      type: 'bar',
      barMaxWidth: 16,
      showBackground: true,
      backgroundStyle: { color: '#f2f5f9', borderRadius: 5 },
      data: chartRows.value.map((item, index) => ({
        value: item.headcount,
        itemStyle: { color: index ? '#2563eb' : '#0f9f8f', borderRadius: [0, 5, 5, 0] },
      })),
      label: { show: true, position: 'right', distance: 9, color: '#27364b', fontWeight: 700, formatter: '{c} 人' },
    }],
  }
  const pieOption: EChartsOption = {
    animation: !reduceMotion,
    animationDuration: 420,
    color: chartRows.value.map((item) => item.color),
    title: {
      text: String(total.value),
      subtext: '在职员工',
      left: 'center',
      top: '31%',
      textStyle: { color: '#10233f', fontSize: 30, fontWeight: 700, fontFamily: 'Bahnschrift, Microsoft YaHei' },
      subtextStyle: { color: '#768397', fontSize: 12, lineHeight: 22 },
    },
    tooltip: {
      ...tooltip,
      trigger: 'item',
      formatter: (item: any) => '<strong>' + item.name + '</strong><br/>在职人数　'
        + item.value + ' 人<br/>组织占比　' + item.percent + '%',
    },
    legend: {
      type: 'scroll',
      bottom: 0,
      left: 'center',
      width: '92%',
      icon: 'circle',
      itemWidth: 7,
      itemHeight: 7,
      itemGap: 14,
      textStyle: { color: '#5f6f84', fontSize: 11 },
      pageIconColor: '#2563eb',
      pageIconInactiveColor: '#cbd5e1',
      pageTextStyle: { color: '#8390a3' },
    },
    series: [{
      type: 'pie',
      radius: ['58%', '76%'],
      center: ['50%', '39%'],
      padAngle: 2,
      minAngle: 3,
      itemStyle: { borderColor: '#fff', borderWidth: 2, borderRadius: 4 },
      label: { show: false },
      emphasis: { scaleSize: 5 },
      data: chartRows.value.map((item) => ({ name: item.departmentName, value: item.headcount })),
    }],
  }
  barChart.setOption(barOption, true)
  pieChart.setOption(pieOption, true)
}

async function load() {
  loading.value = true
  let loaded = false
  try {
    departments.value = await fetchDepartmentHeadcounts()
    loaded = true
  } catch {
    ElMessage.error('无法加载人员分析数据')
  } finally {
    loading.value = false
  }
  if (loaded) {
    await nextTick()
    renderCharts()
  }
}

const resize = () => {
  barChart?.resize()
  pieChart?.resize()
}

watch(chartRows, async () => {
  await nextTick()
  renderCharts()
})

onMounted(async () => {
  addEventListener('resize', resize)
  await load()
})

onBeforeUnmount(() => {
  removeEventListener('resize', resize)
  barChart?.dispose()
  pieChart?.dispose()
})
</script>

<template>
  <PageFrame title="人员分析" description="从部门规模和人员结构两个维度，快速了解当前组织的人力分布。">
    <template #actions>
      <el-button class="subtle-button" @click="router.push('/reports/attendance')">假勤统计</el-button>
      <el-button type="primary" :loading="loading" @click="load">刷新数据</el-button>
    </template>

    <section v-loading="loading" class="overview-card">
      <div class="overview-card__primary">
        <p><i></i>组织人员概览</p>
        <div><strong>{{ total }}</strong><span>名在职员工</span></div>
        <small>{{ summary }}</small>
      </div>
      <div class="overview-card__metrics" aria-label="组织人员关键指标">
        <div><span>统计部门</span><strong>{{ rows.length }}<small>个</small></strong><em>已纳入人员统计</em></div>
        <div><span>部门平均人数</span><strong>{{ average }}<small>人</small></strong><em>当前组织平均规模</em></div>
        <div><span>前三部门占比</span><strong>{{ percent(topThreeShare) }}</strong><em>人员集中度参考</em></div>
      </div>
    </section>

    <template v-if="!loading && chartRows.length">
      <section class="chart-grid">
        <article class="panel panel--wide">
          <header class="panel__head">
            <div class="panel__title"><b>01</b><div><h2>部门人数排名</h2><p>按在职人数由高到低排列，识别人员主要分布区域。</p></div></div>
            <span class="pill">共 {{ rows.length }} 个部门</span>
          </header>
          <div ref="barEl" class="chart chart--bar" role="img" aria-label="各部门在职人数横向柱状图"></div>
        </article>
        <article class="panel panel--soft">
          <header class="panel__head">
            <div class="panel__title"><b class="teal">02</b><div><h2>人员结构占比</h2><p>查看各部门对组织总人数的贡献比例。</p></div></div>
          </header>
          <div ref="pieEl" class="chart chart--pie" role="img" aria-label="各部门在职人数占比环形图"></div>
          <div class="spread"><div><span>规模差值</span><small>最大与最小部门人数之差</small></div><strong>{{ spread }} 人</strong></div>
        </article>
      </section>

      <section class="detail">
        <header><div><span>DEPARTMENT DETAIL</span><h2>部门人员明细</h2></div><p>精确人数与组织占比</p></header>
        <div class="table-wrap">
          <div class="data-head"><span>排名</span><span>部门</span><span>人数</span><span>组织占比</span></div>
          <div v-for="(item, index) in chartRows" :key="item.departmentName" class="data-row">
            <span class="rank" :class="{ top: index < 3 }">{{ String(index + 1).padStart(2, '0') }}</span>
            <span class="name"><i :style="{ background: item.color }"></i>{{ item.departmentName }}</span>
            <strong>{{ item.headcount }} 人</strong>
            <span class="share"><i><b :style="{ width: percent(item.share), background: item.color }"></b></i><em>{{ percent(item.share) }}</em></span>
          </div>
        </div>
      </section>
    </template>
    <EmptyState v-else-if="!loading" title="暂无人员数据" description="创建启用部门并完成员工入职后，这里将自动生成组织人员分析。" />
  </PageFrame>
</template>

<style scoped>
.overview-card{display:grid;grid-template-columns:minmax(330px,.9fr) minmax(0,1.7fr);min-height:166px;margin-bottom:18px;overflow:hidden;border:1px solid #e0e6ee;border-radius:12px;background:#fff;box-shadow:0 8px 24px rgba(35,55,80,.045)}
.overview-card__primary{display:flex;flex-direction:column;justify-content:center;padding:25px 28px;border-right:1px solid #dce6f2;background:linear-gradient(135deg,#f2f7ff 0%,#f8fbff 100%)}
.overview-card__primary>p,.detail header span{display:flex;align-items:center;gap:8px;margin:0 0 12px;color:#2563eb;font-size:11px;font-weight:700;letter-spacing:.08em}
.overview-card__primary>p i{width:7px;height:7px;border-radius:2px;background:#2563eb;box-shadow:0 0 0 4px rgba(37,99,235,.1)}
.overview-card__primary>div{display:flex;align-items:baseline;gap:10px}.overview-card__primary>div strong{color:#17283f;font:700 44px/1 Bahnschrift,'Microsoft YaHei';letter-spacing:-.03em}.overview-card__primary>div span{color:#40536b;font-size:15px;font-weight:600}
.overview-card__primary>small{max-width:540px;margin-top:13px;color:#68798e;font-size:12px;line-height:1.65}
.overview-card__metrics{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));align-items:stretch;padding:18px 12px}
.overview-card__metrics>div{position:relative;display:flex;flex-direction:column;justify-content:center;min-width:0;padding:14px 24px}.overview-card__metrics>div+div:before{position:absolute;top:18px;bottom:18px;left:0;width:1px;background:#e8edf3;content:''}
.overview-card__metrics span{margin-bottom:11px;color:#718096;font-size:12px}.overview-card__metrics strong{color:#17283f;font:700 28px/1 Bahnschrift,'Microsoft YaHei'}.overview-card__metrics strong small{margin-left:4px;color:#718096;font:500 12px 'Microsoft YaHei'}.overview-card__metrics em{margin-top:10px;color:#9aa5b3;font-size:11px;font-style:normal}
.chart-grid{display:grid;grid-template-columns:minmax(0,1.55fr) minmax(340px,.72fr);gap:18px}.panel,.detail{min-width:0;border:1px solid #e0e6ee;border-radius:12px;background:#fff;box-shadow:0 8px 24px rgba(35,55,80,.045)}
.panel{padding:24px 26px 20px}.panel--soft{background:linear-gradient(180deg,#fff,#f8fafc)}
.panel__head,.panel__title{display:flex;align-items:flex-start}.panel__head{justify-content:space-between;gap:20px;margin-bottom:8px}.panel__title{gap:13px}
.panel__title>b{display:grid;flex:0 0 32px;width:32px;height:32px;place-items:center;border-radius:8px;color:#2563eb;background:#eaf1ff;font:700 11px Bahnschrift}.panel__title>b.teal{color:#087e73;background:#e5f7f4}
.panel h2,.detail h2{margin:1px 0 5px;color:#17283f;font-size:17px}.panel p,.detail p{margin:0;color:#728095;font-size:12px;line-height:1.6}.pill{flex:none;padding:6px 10px;border:1px solid #dce4ee;border-radius:999px;color:#64748b;background:#f8fafc;font-size:11px}
.chart{width:100%}.chart--bar{height:390px}.chart--pie{height:328px}
.spread{display:flex;align-items:center;justify-content:space-between;gap:16px;padding:15px 17px;border:1px solid #e4eaf1;border-radius:9px;background:#fff}.spread div{display:flex;flex-direction:column;gap:4px}.spread span,.spread small{color:#738196;font-size:11px}.spread strong{color:#10233f;font:700 20px Bahnschrift,'Microsoft YaHei'}
.detail{margin-top:18px;overflow:hidden}.detail>header{display:flex;align-items:flex-end;justify-content:space-between;padding:20px 24px 17px;border-bottom:1px solid #e8edf3}.detail header span{display:block;margin-bottom:5px;color:#2563eb;font-size:9px}
.table-wrap{overflow-x:auto}.data-head,.data-row{display:grid;grid-template-columns:80px minmax(180px,1.2fr) 130px minmax(220px,.9fr);align-items:center;min-width:720px;padding:0 24px}.data-head{min-height:42px;color:#8793a4;background:#fafbfd;font-size:11px;font-weight:600}
.data-row{min-height:58px;border-top:1px solid #edf1f5;color:#334155;font-size:13px;transition:background-color .18s ease}.data-row:hover{background:#f8fbff}.rank{color:#8b96a7;font:12px Bahnschrift}.rank.top{color:#2563eb;font-weight:700}
.name{display:flex;align-items:center;gap:10px;color:#26374d;font-weight:600}.name i{width:7px;height:7px;border-radius:50%}.data-row>strong{color:#17283f}
.share{display:grid;grid-template-columns:minmax(100px,1fr) 48px;gap:14px;align-items:center}.share>i{height:5px;overflow:hidden;border-radius:99px;background:#edf1f5}.share>i b{display:block;height:100%;border-radius:inherit}.share em{color:#5f6f84;font-size:12px;font-style:normal;font-weight:600;text-align:right}
.subtle-button{border-color:#dce3ec;color:#40526a}
@media(max-width:1180px){.overview-card{grid-template-columns:1fr}.overview-card__primary{border-right:0;border-bottom:1px solid #dce6f2}.chart-grid{grid-template-columns:1fr}.chart--pie{height:370px}}
@media(max-width:680px){.overview-card__primary{padding:22px 20px}.overview-card__primary>div strong{font-size:40px}.overview-card__metrics{grid-template-columns:1fr;padding:8px 16px}.overview-card__metrics>div{display:grid;grid-template-columns:minmax(0,1fr) auto;gap:4px 16px;align-items:center;padding:16px 4px}.overview-card__metrics>div+div:before{inset:0 4px auto;width:auto;height:1px}.overview-card__metrics span{margin:0}.overview-card__metrics strong{grid-row:span 2;font-size:24px}.overview-card__metrics em{margin:0}.panel{padding:20px 16px 16px}.panel__head{flex-direction:column}.pill{margin-left:45px}.chart--bar{height:340px}.detail>header{align-items:flex-start;flex-direction:column;gap:8px}}
@media(prefers-reduced-motion:reduce){.data-row{transition:none}}
</style>

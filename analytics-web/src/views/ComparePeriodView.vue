<template>
  <div>
    <div class="subnav">
      <router-link to="/compare/period" class="active">Period</router-link>
      <router-link to="/compare/platform">Platform</router-link>
      <router-link to="/compare/pages">Pages</router-link>
    </div>

    <div class="range-row">
      <div class="range-field">
        <label>Primary</label>
        <el-date-picker
          v-model="primaryRange"
          type="daterange"
          range-separator="–"
          value-format="YYYY-MM-DD"
          size="small"
          @change="syncCompareLength"
        />
      </div>
      <div class="range-field">
        <label>Compare</label>
        <el-date-picker
          v-model="compareRange"
          type="daterange"
          range-separator="–"
          value-format="YYYY-MM-DD"
          size="small"
        />
      </div>
    </div>

    <template v-if="loading">
      <div class="kpi-strip">
        <div v-for="i in 4" :key="i" class="kpi-cell">
          <div class="skeleton skeleton-line" style="width: 64px" />
          <div class="skeleton skeleton-kpi" />
        </div>
      </div>
    </template>
    <template v-else>
      <div class="kpi-strip">
        <div v-for="card in cards" :key="card.label" class="kpi-cell">
          <div class="kpi-label">{{ card.label }}</div>
          <div class="kpi-value">{{ card.current }}</div>
          <div class="kpi-sub">vs {{ card.compare }} · <span :class="card.deltaClass">{{ card.deltaText }}</span></div>
        </div>
      </div>

      <div class="chart-block">
        <v-chart v-if="hasChart" class="chart" :option="chartOption" autoresize />
        <div v-else class="empty-state">暂无数据</div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import VChart from 'vue-echarts'
import { getDashboard } from '@/api/dashboardApi'
import { useSiteStore } from '@/stores/siteStore'
import { useFilterStore } from '@/stores/filterStore'

use([CanvasRenderer, LineChart, GridComponent, TooltipComponent, LegendComponent])

const siteStore = useSiteStore()
const filter = useFilterStore()
const loading = ref(false)
const primary = ref(null)
const compare = ref(null)
const primaryTrend = ref([])
const compareTrend = ref([])
const primaryRange = ref(null)
const compareRange = ref(null)

const hasChart = computed(() => primaryTrend.value.length || compareTrend.value.length)

const cards = computed(() => {
  const cur = primary.value?.kpi || {}
  const cmp = compare.value?.kpi || {}
  const defs = [
    { key: 'clicks', label: 'Clicks', fmt: (v) => Number(v || 0).toLocaleString() },
    { key: 'impressions', label: 'Impressions', fmt: (v) => Number(v || 0).toLocaleString() },
    { key: 'ctr', label: 'CTR', fmt: (v) => `${(Number(v || 0) * 100).toFixed(2)}%` },
    { key: 'avgPosition', label: 'Avg. position', fmt: (v) => Number(v || 0).toFixed(1) },
  ]
  return defs.map((d) => {
    const a = Number(cur[d.key] || 0)
    const b = Number(cmp[d.key] || 0)
    const delta = b === 0 ? (a === 0 ? null : 100) : ((a - b) / Math.abs(b)) * 100
    const better = d.key === 'avgPosition' ? delta != null && delta <= 0 : delta != null && delta >= 0
    return {
      label: d.label,
      current: d.fmt(a),
      compare: d.fmt(b),
      deltaText: delta == null ? '—' : `${delta >= 0 ? '↑' : '↓'} ${Math.abs(delta).toFixed(1)}%`,
      deltaClass: delta == null ? 'neutral' : better ? 'up' : 'down',
    }
  })
})

const chartOption = computed(() => {
  const p = primaryTrend.value
  const c = compareTrend.value
  const len = Math.max(p.length, c.length)
  const labels = Array.from({ length: len }, (_, i) => `D${i + 1}`)
  return {
    color: ['#6366f1', '#9ca3af'],
    legend: { data: ['Primary', 'Compare'], top: 0, right: 0, textStyle: { color: '#6b7280', fontSize: 12 }, itemWidth: 12, itemHeight: 2 },
    tooltip: { trigger: 'axis', backgroundColor: '#fff', borderColor: '#e5e7eb', borderWidth: 1, textStyle: { color: '#111827', fontSize: 12 } },
    grid: { left: 8, right: 8, top: 28, bottom: 8, containLabel: true },
    xAxis: { type: 'category', data: labels, axisLine: { show: false }, axisTick: { show: false }, axisLabel: { color: '#6b7280', fontSize: 11 } },
    yAxis: { type: 'value', splitLine: { lineStyle: { color: '#f3f4f6' } }, axisLabel: { color: '#6b7280', fontSize: 11 } },
    series: [
      { name: 'Primary', type: 'line', smooth: 0.15, showSymbol: false, lineStyle: { width: 2, color: '#6366f1' }, areaStyle: { color: 'rgba(99,102,241,0.1)' }, data: p.map((r) => Number(r.clicks || 0)) },
      { name: 'Compare', type: 'line', smooth: 0.15, showSymbol: false, lineStyle: { width: 2, color: '#9ca3af', type: 'dashed' }, data: c.map((r) => Number(r.clicks || 0)) },
    ],
  }
})

function syncCompareLength() {
  if (!primaryRange.value?.length) return
  const [s, e] = primaryRange.value
  const days = Math.round((new Date(e) - new Date(s)) / 86400000) + 1
  const prevEnd = new Date(s)
  prevEnd.setDate(prevEnd.getDate() - 1)
  const prevStart = new Date(prevEnd)
  prevStart.setDate(prevEnd.getDate() - (days - 1))
  const fmt = (d) => d.toISOString().slice(0, 10)
  compareRange.value = [fmt(prevStart), fmt(prevEnd)]
  load()
}

async function load() {
  if (!siteStore.currentSiteId) return
  if (!primaryRange.value?.length || !compareRange.value?.length) {
    // seed from global filter
    const { start, end } = filter.dateRange
    const prev = filter.previousRange
    primaryRange.value = [start, end]
    compareRange.value = [prev.start, prev.end]
  }
  loading.value = true
  try {
    const base = { siteId: siteStore.currentSiteId, platform: filter.platform || undefined }
    const [a, b] = await Promise.all([
      getDashboard({ ...base, startDate: primaryRange.value[0], endDate: primaryRange.value[1] }),
      getDashboard({ ...base, startDate: compareRange.value[0], endDate: compareRange.value[1] }),
    ])
    primary.value = a.data
    compare.value = b.data
    primaryTrend.value = a.data?.trend || []
    compareTrend.value = b.data?.trend || []
  } finally {
    loading.value = false
  }
}

watch(
  () => [siteStore.currentSiteId, filter.platform, filter.dateRange.start, filter.dateRange.end],
  () => {
    primaryRange.value = null
    load()
  },
  { immediate: true }
)

watch(compareRange, () => {
  if (compareRange.value?.length === 2 && primaryRange.value?.length === 2) load()
})
</script>

<style scoped>
.range-row {
  display: flex;
  gap: 24px;
  flex-wrap: wrap;
  margin-bottom: 20px;
}
.range-field label {
  display: block;
  font-size: 12px;
  font-weight: 500;
  color: #6b7280;
  margin-bottom: 6px;
}
.kpi-strip {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  margin-bottom: 24px;
  overflow: hidden;
}
.kpi-cell { padding: 20px 24px; border-right: 1px solid #e5e7eb; }
.kpi-cell:last-child { border-right: none; }
.kpi-label { font-size: 12px; font-weight: 500; color: #6b7280; }
.kpi-value { font-size: 28px; font-weight: 600; font-variant-numeric: tabular-nums; letter-spacing: -0.02em; margin: 8px 0 6px; }
.kpi-sub { font-size: 12px; color: #6b7280; }
.up { color: #16a34a; }
.down { color: #dc2626; }
.neutral { color: #6b7280; }
.chart { height: 240px; width: 100%; }
</style>

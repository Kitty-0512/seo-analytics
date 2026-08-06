<template>
  <div>
    <div class="subnav">
      <router-link to="/compare/period">Period</router-link>
      <router-link to="/compare/platform" class="active">Platform</router-link>
      <router-link to="/compare/pages">Pages</router-link>
    </div>

    <template v-if="loading">
      <div class="cols">
        <div class="panel skeleton" style="height: 180px" />
        <div class="panel skeleton" style="height: 180px" />
      </div>
    </template>
    <template v-else>
      <div class="cols">
        <div class="panel">
          <div class="panel-title">GSC</div>
          <div class="metric-row" v-for="m in metrics(gsc)" :key="'g' + m.label">
            <span>{{ m.label }}</span>
            <span class="num">{{ m.value }}</span>
          </div>
        </div>
        <div class="panel">
          <div class="panel-title">Bing</div>
          <div class="metric-row" v-for="m in metrics(bing)" :key="'b' + m.label">
            <span>{{ m.label }}</span>
            <span class="num">{{ m.value }}</span>
          </div>
        </div>
      </div>

      <v-chart v-if="hasChart" class="chart" :option="chartOption" autoresize />
      <div v-else class="empty-state">暂无数据</div>
    </template>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { BarChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import VChart from 'vue-echarts'
import { comparePlatform } from '@/api/dashboardApi'
import { useSiteStore } from '@/stores/siteStore'
import { useFilterStore } from '@/stores/filterStore'

use([CanvasRenderer, BarChart, GridComponent, TooltipComponent, LegendComponent])

const siteStore = useSiteStore()
const filter = useFilterStore()
const loading = ref(false)
const gsc = ref({})
const bing = ref({})
const trendByPlatform = ref({ GSC: [], BING: [] })

const hasChart = computed(() =>
  Number(gsc.value.clicks || 0) + Number(bing.value.clicks || 0) > 0
)

function metrics(kpi) {
  return [
    { label: 'Clicks', value: Number(kpi.clicks || 0).toLocaleString() },
    { label: 'Impressions', value: Number(kpi.impressions || 0).toLocaleString() },
    { label: 'CTR', value: `${(Number(kpi.ctr || 0) * 100).toFixed(2)}%` },
    { label: 'Keywords', value: Number(kpi.keywordCount || 0).toLocaleString() },
  ]
}

const chartOption = computed(() => ({
  color: ['#6366f1', '#f59e0b'],
  legend: { data: ['GSC', 'Bing'], top: 0, right: 0, textStyle: { color: '#6b7280', fontSize: 12 } },
  tooltip: { trigger: 'axis', backgroundColor: '#fff', borderColor: '#e5e7eb', borderWidth: 1, textStyle: { color: '#111827', fontSize: 12 } },
  grid: { left: 8, right: 8, top: 28, bottom: 8, containLabel: true },
  xAxis: {
    type: 'category',
    data: ['Clicks', 'Impressions', 'Keywords'],
    axisLine: { show: false },
    axisTick: { show: false },
    axisLabel: { color: '#6b7280', fontSize: 12 },
  },
  yAxis: {
    type: 'value',
    splitLine: { lineStyle: { color: '#f3f4f6' } },
    axisLabel: { color: '#6b7280', fontSize: 11 },
  },
  series: [
    {
      name: 'GSC',
      type: 'bar',
      barWidth: 18,
      itemStyle: { color: '#6366f1', borderRadius: [2, 2, 0, 0] },
      emphasis: { itemStyle: { color: '#4f46e5' } },
      data: [
        Number(gsc.value.clicks || 0),
        Number(gsc.value.impressions || 0),
        Number(gsc.value.keywordCount || 0),
      ],
    },
    {
      name: 'Bing',
      type: 'bar',
      barWidth: 18,
      itemStyle: { color: '#f59e0b', borderRadius: [2, 2, 0, 0] },
      emphasis: { itemStyle: { color: '#d97706' } },
      data: [
        Number(bing.value.clicks || 0),
        Number(bing.value.impressions || 0),
        Number(bing.value.keywordCount || 0),
      ],
    },
  ],
}))

async function load() {
  if (!siteStore.currentSiteId) return
  loading.value = true
  try {
    const res = await comparePlatform({
      siteId: siteStore.currentSiteId,
      startDate: filter.dateRange.start,
      endDate: filter.dateRange.end,
    })
    gsc.value = res.data?.gsc || {}
    bing.value = res.data?.bing || {}
    trendByPlatform.value = res.data?.trendByPlatform || { GSC: [], BING: [] }
  } finally {
    loading.value = false
  }
}

watch(
  () => [siteStore.currentSiteId, filter.dateRange.start, filter.dateRange.end, filter.period, filter.customRange],
  () => load(),
  { immediate: true, deep: true }
)
</script>

<style scoped>
.cols {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-bottom: 24px;
}
.metric-row {
  display: flex;
  justify-content: space-between;
  padding: 10px 0;
  border-bottom: 1px solid #f3f4f6;
  font-size: 14px;
  color: #6b7280;
}
.metric-row .num { color: #111827; font-weight: 500; }
.chart { height: 280px; width: 100%; }
@media (max-width: 720px) {
  .cols { grid-template-columns: 1fr; }
}
</style>

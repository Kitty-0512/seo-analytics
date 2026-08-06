<template>
  <div class="dashboard">
    <template v-if="loading">
      <div class="kpi-strip">
        <div v-for="i in 4" :key="i" class="kpi-cell">
          <div class="skeleton skeleton-line" style="width: 72px" />
          <div class="skeleton skeleton-kpi" />
          <div class="skeleton skeleton-line" style="width: 40px" />
        </div>
      </div>
      <div class="skeleton" style="height: 240px; margin-bottom: 32px" />
    </template>

    <template v-else>
      <div class="kpi-strip">
        <div v-for="card in kpiCards" :key="card.label" class="kpi-cell">
          <div class="kpi-label">{{ card.label }}</div>
          <div class="kpi-value">{{ card.value }}</div>
          <div class="kpi-delta" :class="card.deltaClass">
            <template v-if="card.delta != null">
              {{ card.delta >= 0 ? '↑' : '↓' }} {{ Math.abs(card.delta).toFixed(1) }}%
            </template>
            <template v-else>—</template>
          </div>
        </div>
      </div>

      <div class="chart-block">
        <v-chart v-if="hasTrend" class="chart" :option="chartOption" autoresize />
        <div v-else class="empty-state">暂无数据</div>
      </div>

      <h2 class="section-title">Top keywords</h2>
      <table v-if="topKeywords.length" class="plausible-table">
        <thead>
          <tr>
            <th @click="toggleSort('keyword')">Keyword</th>
            <th class="right" style="width: 120px" @click="toggleSort('clicks')">Clicks</th>
            <th class="right" style="width: 120px" @click="toggleSort('impressions')">Impressions</th>
            <th class="right" style="width: 100px" @click="toggleSort('position')">Pos.</th>
            <th class="right" style="width: 120px" @click="toggleSort('share')">Share</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in sortedKeywords" :key="row.keyword">
            <td><div class="cell-pad">{{ row.keyword }}</div></td>
            <td class="right"><span class="num">{{ formatNum(row.clicks) }}</span></td>
            <td class="right"><span class="num">{{ formatNum(row.impressions) }}</span></td>
            <td class="right">
              <span class="num">{{ formatNum(row.avg_position ?? row.avgPosition, 1) }}</span>
            </td>
            <td class="right">
              <div class="bar-inline">
                <span class="num">{{ sharePct(row.clicks) }}</span>
                <div class="bar-track">
                  <div class="bar-fill" :style="{ width: barWidth(row.clicks) }" />
                </div>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
      <div v-else class="empty-state">暂无数据</div>
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
const kpi = ref({ clicks: 0, impressions: 0, ctr: 0, avgPosition: 0 })
const prevKpi = ref(null)
const trendByPlatform = ref({ GSC: [], BING: [] })
const topKeywords = ref([])
const sortKey = ref('clicks')
const sortAsc = ref(false)

const maxClicks = computed(() => Math.max(...topKeywords.value.map((r) => Number(r.clicks || 0)), 1))
const hasTrend = computed(() =>
  (trendByPlatform.value.GSC?.length || 0) + (trendByPlatform.value.BING?.length || 0) > 0
)

const sortedKeywords = computed(() => {
  const list = [...topKeywords.value]
  list.sort((a, b) => {
    let av
    let bv
    if (sortKey.value === 'keyword') {
      return sortAsc.value
        ? String(a.keyword).localeCompare(String(b.keyword))
        : String(b.keyword).localeCompare(String(a.keyword))
    }
    if (sortKey.value === 'position') {
      av = Number(a.avg_position ?? a.avgPosition ?? 0)
      bv = Number(b.avg_position ?? b.avgPosition ?? 0)
    } else if (sortKey.value === 'share') {
      av = Number(a.clicks || 0)
      bv = Number(b.clicks || 0)
    } else {
      av = Number(a[sortKey.value] || 0)
      bv = Number(b[sortKey.value] || 0)
    }
    return sortAsc.value ? av - bv : bv - av
  })
  return list
})

const kpiCards = computed(() => {
  const defs = [
    { key: 'clicks', label: 'Total clicks', value: formatNum(kpi.value.clicks), raw: Number(kpi.value.clicks || 0) },
    { key: 'impressions', label: 'Impressions', value: formatNum(kpi.value.impressions), raw: Number(kpi.value.impressions || 0) },
    { key: 'ctr', label: 'CTR', value: `${(Number(kpi.value.ctr || 0) * 100).toFixed(2)}%`, raw: Number(kpi.value.ctr || 0) },
    { key: 'avgPosition', label: 'Avg. position', value: formatNum(kpi.value.avgPosition, 1), raw: Number(kpi.value.avgPosition || 0) },
  ]
  return defs.map((item) => {
    const delta = calcDelta(item.key, item.raw)
    const better =
      item.key === 'avgPosition'
        ? delta != null && delta <= 0
        : delta != null && delta >= 0
    return {
      ...item,
      delta,
      deltaClass: delta == null ? 'neutral' : better ? 'up' : 'down',
    }
  })
})

const chartOption = computed(() => {
  const gsc = trendByPlatform.value.GSC || []
  const bing = trendByPlatform.value.BING || []
  const dateSet = new Set([
    ...gsc.map((r) => r.stat_date || r.statDate),
    ...bing.map((r) => r.stat_date || r.statDate),
  ])
  const dates = [...dateSet].sort()
  const mapSeries = (rows) => {
    const m = Object.fromEntries(rows.map((r) => [r.stat_date || r.statDate, Number(r.clicks || 0)]))
    return dates.map((d) => m[d] ?? null)
  }
  return {
    color: ['#6366f1', '#f59e0b'],
    legend: {
      data: ['GSC', 'Bing'],
      top: 0,
      right: 0,
      textStyle: { color: '#6b7280', fontSize: 12 },
      itemWidth: 12,
      itemHeight: 2,
    },
    tooltip: {
      trigger: 'axis',
      backgroundColor: '#fff',
      borderColor: '#e5e7eb',
      borderWidth: 1,
      textStyle: { color: '#111827', fontSize: 12 },
      extraCssText: 'border-radius:6px;box-shadow:none;',
    },
    grid: { left: 8, right: 8, top: 28, bottom: 8, containLabel: true },
    xAxis: {
      type: 'category',
      data: dates,
      boundaryGap: false,
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { color: '#6b7280', fontSize: 11 },
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: '#f3f4f6' } },
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { color: '#6b7280', fontSize: 11 },
    },
    series: [
      {
        name: 'GSC',
        type: 'line',
        smooth: 0.15,
        showSymbol: false,
        connectNulls: true,
        lineStyle: { width: 2, color: '#6366f1' },
        areaStyle: { color: 'rgba(99,102,241,0.1)' },
        data: mapSeries(gsc),
      },
      {
        name: 'Bing',
        type: 'line',
        smooth: 0.15,
        showSymbol: false,
        connectNulls: true,
        lineStyle: { width: 2, color: '#f59e0b' },
        areaStyle: { color: 'rgba(245,158,11,0.1)' },
        data: mapSeries(bing),
      },
    ],
  }
})

function formatNum(v, digits = 0) {
  return Number(v || 0).toLocaleString(undefined, {
    maximumFractionDigits: digits,
    minimumFractionDigits: digits,
  })
}

function calcDelta(key, current) {
  if (!prevKpi.value) return null
  const prev = Number(prevKpi.value[key] || 0)
  if (!prev) return current === 0 ? 0 : 100
  return ((current - prev) / Math.abs(prev)) * 100
}

function barWidth(clicks) {
  return `${Math.max((Number(clicks || 0) / maxClicks.value) * 100, 2)}%`
}

function sharePct(clicks) {
  const total = topKeywords.value.reduce((s, r) => s + Number(r.clicks || 0), 0) || 1
  return `${((Number(clicks || 0) / total) * 100).toFixed(1)}%`
}

function toggleSort(key) {
  if (sortKey.value === key) sortAsc.value = !sortAsc.value
  else {
    sortKey.value = key
    sortAsc.value = key === 'keyword' || key === 'position'
  }
}

async function load() {
  if (!siteStore.currentSiteId) return
  loading.value = true
  try {
    const params = filter.apiParams({ siteId: siteStore.currentSiteId })
    const prev = filter.previousRange
    const [res, prevRes] = await Promise.all([
      getDashboard(params),
      getDashboard({
        ...params,
        startDate: prev.start,
        endDate: prev.end,
      }).catch(() => null),
    ])
    const data = res.data || {}
    kpi.value = data.kpi || { clicks: 0, impressions: 0, ctr: 0, avgPosition: 0 }
    trendByPlatform.value = data.trendByPlatform || { GSC: [], BING: [] }
    topKeywords.value = data.topKeywords || []
    prevKpi.value = prevRes?.data?.kpi || null
  } finally {
    loading.value = false
  }
}

watch(
  () => [siteStore.currentSiteId, filter.period, filter.platform, filter.customRange, filter.dateRange.start, filter.dateRange.end],
  () => load(),
  { immediate: true, deep: true }
)
</script>

<style scoped>
.kpi-strip {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  margin-bottom: 24px;
  overflow: hidden;
}

.kpi-cell {
  padding: 20px 24px;
  border-right: 1px solid #e5e7eb;
}

.kpi-cell:last-child { border-right: none; }

.kpi-label {
  font-size: 12px;
  font-weight: 500;
  color: #6b7280;
}

.kpi-value {
  font-size: 28px;
  line-height: 1.2;
  margin: 8px 0 6px;
}

.kpi-delta {
  font-size: 12px;
  font-weight: 500;
  font-variant-numeric: tabular-nums;
}

.kpi-delta.up { color: #16a34a; }
.kpi-delta.down { color: #dc2626; }
.kpi-delta.neutral { color: #6b7280; }

.chart-block { margin-bottom: 32px; }
.chart { height: 240px; width: 100%; }

@media (max-width: 800px) {
  .kpi-strip { grid-template-columns: repeat(2, 1fr); }
  .kpi-cell:nth-child(2) { border-right: none; }
  .kpi-cell:nth-child(1),
  .kpi-cell:nth-child(2) { border-bottom: 1px solid #e5e7eb; }
}
</style>

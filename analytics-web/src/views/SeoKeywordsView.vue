<template>
  <div>
    <div class="subnav">
      <router-link to="/seo/keywords" class="active">Keywords</router-link>
      <router-link to="/seo/pages">Pages</router-link>
      <router-link to="/seo/query-page">Query–Page</router-link>
      <router-link to="/seo/opportunities">Opportunities</router-link>
    </div>

    <template v-if="loading">
      <div v-for="i in 8" :key="i" class="skeleton skeleton-line" style="height: 28px; margin-bottom: 8px" />
    </template>

    <template v-else>
      <table v-if="keywords.length" class="plausible-table">
        <thead>
          <tr>
            <th @click="toggleSort('keyword')">Keyword</th>
            <th class="right" style="width: 100px" @click="toggleSort('clicks')">Clicks</th>
            <th class="right" style="width: 110px" @click="toggleSort('impressions')">Impressions</th>
            <th class="right" style="width: 80px" @click="toggleSort('position')">Pos.</th>
            <th class="right" style="width: 120px">Share</th>
          </tr>
        </thead>
        <tbody>
          <template v-for="row in sorted" :key="row.keyword">
            <tr @click="toggleExpand(row.keyword)">
              <td>
                <div class="cell-pad">
                  <span class="expand">{{ expanded === row.keyword ? '▾' : '▸' }}</span>
                  {{ row.keyword }}
                </div>
              </td>
              <td class="right"><span class="num">{{ formatNum(row.clicks) }}</span></td>
              <td class="right"><span class="num">{{ formatNum(row.impressions) }}</span></td>
              <td class="right">
                <span class="num">{{ Number(row.avg_position ?? row.avgPosition ?? 0).toFixed(1) }}</span>
              </td>
              <td class="right">
                <div class="bar-inline">
                  <span class="num">{{ sharePct(row.clicks) }}</span>
                  <div class="bar-track"><div class="bar-fill" :style="{ width: barWidth(row.clicks) }" /></div>
                </div>
              </td>
            </tr>
            <tr v-if="expanded === row.keyword" class="expand-row">
              <td colspan="5">
                <div class="expand-box">
                  <div v-if="trendLoading" class="skeleton" style="height: 160px" />
                  <v-chart v-else-if="trend.length" class="mini-chart" :option="trendOption" autoresize />
                  <div v-else class="empty-state" style="padding: 16px 0">暂无数据</div>
                </div>
              </td>
            </tr>
          </template>
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
import { GridComponent, TooltipComponent } from 'echarts/components'
import VChart from 'vue-echarts'
import { getKeywordTrend, getSeoData } from '@/api/dashboardApi'
import { useSiteStore } from '@/stores/siteStore'
import { useFilterStore } from '@/stores/filterStore'

use([CanvasRenderer, LineChart, GridComponent, TooltipComponent])

const siteStore = useSiteStore()
const filter = useFilterStore()
const loading = ref(false)
const keywords = ref([])
const sortKey = ref('clicks')
const sortAsc = ref(false)
const expanded = ref(null)
const trend = ref([])
const trendLoading = ref(false)

const maxClicks = computed(() => Math.max(...keywords.value.map((r) => Number(r.clicks || 0)), 1))

const sorted = computed(() => {
  const list = [...keywords.value]
  list.sort((a, b) => {
    if (sortKey.value === 'keyword') {
      return sortAsc.value
        ? String(a.keyword).localeCompare(String(b.keyword))
        : String(b.keyword).localeCompare(String(a.keyword))
    }
    const av = sortKey.value === 'position'
      ? Number(a.avg_position ?? a.avgPosition ?? 0)
      : Number(a[sortKey.value] || 0)
    const bv = sortKey.value === 'position'
      ? Number(b.avg_position ?? b.avgPosition ?? 0)
      : Number(b[sortKey.value] || 0)
    return sortAsc.value ? av - bv : bv - av
  })
  return list
})

const trendOption = computed(() => ({
  color: ['#6366f1'],
  tooltip: {
    trigger: 'axis',
    backgroundColor: '#fff',
    borderColor: '#e5e7eb',
    borderWidth: 1,
    textStyle: { color: '#111827', fontSize: 12 },
  },
  grid: { left: 8, right: 8, top: 16, bottom: 8, containLabel: true },
  xAxis: {
    type: 'category',
    data: trend.value.map((r) => r.stat_date || r.statDate),
    boundaryGap: false,
    axisLine: { show: false },
    axisTick: { show: false },
    axisLabel: { color: '#6b7280', fontSize: 11 },
  },
  yAxis: {
    type: 'value',
    inverse: true,
    splitLine: { lineStyle: { color: '#f3f4f6' } },
    axisLabel: { color: '#6b7280', fontSize: 11 },
  },
  series: [{
    type: 'line',
    smooth: 0.15,
    showSymbol: false,
    lineStyle: { width: 2 },
    areaStyle: { color: 'rgba(99,102,241,0.1)' },
    data: trend.value.map((r) => Number(r.avg_position ?? r.avgPosition ?? 0)),
  }],
}))

function formatNum(v) { return Number(v || 0).toLocaleString() }
function barWidth(c) { return `${Math.max((Number(c || 0) / maxClicks.value) * 100, 2)}%` }
function sharePct(c) {
  const t = keywords.value.reduce((s, r) => s + Number(r.clicks || 0), 0) || 1
  return `${((Number(c || 0) / t) * 100).toFixed(1)}%`
}
function toggleSort(key) {
  if (sortKey.value === key) sortAsc.value = !sortAsc.value
  else { sortKey.value = key; sortAsc.value = key === 'keyword' || key === 'position' }
}

async function toggleExpand(keyword) {
  if (expanded.value === keyword) {
    expanded.value = null
    return
  }
  expanded.value = keyword
  trendLoading.value = true
  try {
    const res = await getKeywordTrend(filter.apiParams({
      siteId: siteStore.currentSiteId,
      keyword,
    }))
    trend.value = res.data || []
  } finally {
    trendLoading.value = false
  }
}

async function load() {
  if (!siteStore.currentSiteId) return
  loading.value = true
  expanded.value = null
  try {
    const res = await getSeoData(filter.apiParams({
      siteId: siteStore.currentSiteId,
      keywordLimit: 100,
      pageLimit: 1,
    }))
    keywords.value = res.data?.keywords || []
  } finally {
    loading.value = false
  }
}

watch(
  () => [siteStore.currentSiteId, filter.period, filter.platform, filter.customRange, filter.dateRange.start],
  () => load(),
  { immediate: true, deep: true }
)
</script>

<style scoped>
.expand {
  display: inline-block;
  width: 14px;
  color: #6b7280;
  font-size: 10px;
}
.expand-row { cursor: default; }
.expand-row:hover { background: transparent !important; }
.expand-box { padding: 8px 0 16px; }
.mini-chart { height: 160px; width: 100%; }
</style>

<template>
  <div>
    <div class="subnav">
      <router-link to="/compare/period">Period</router-link>
      <router-link to="/compare/platform">Platform</router-link>
      <router-link to="/compare/pages" class="active">Pages</router-link>
    </div>

    <div class="picker">
      <el-select
        v-model="selected"
        multiple
        filterable
        collapse-tags
        collapse-tags-tooltip
        placeholder="Select up to 5 pages"
        style="width: 100%; max-width: 640px"
        size="small"
        @change="onSelect"
      >
        <el-option
          v-for="p in pageOptions"
          :key="p"
          :label="p"
          :value="p"
        />
      </el-select>
    </div>

    <template v-if="loading">
      <div class="skeleton" style="height: 240px; margin-bottom: 24px" />
    </template>
    <template v-else>
      <v-chart v-if="series.length" class="chart" :option="chartOption" autoresize />
      <div v-else class="empty-state">暂无数据</div>

      <h2 class="section-title" style="margin-top: 24px">Metrics</h2>
      <table v-if="table.length" class="plausible-table">
        <thead>
          <tr>
            <th>Page</th>
            <th class="right" style="width: 100px">Clicks</th>
            <th class="right" style="width: 110px">Impressions</th>
            <th class="right" style="width: 80px">CTR</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in table" :key="row.pageUrl">
            <td><div class="cell-pad">{{ row.pageUrl }}</div></td>
            <td class="right"><span class="num">{{ Number(row.clicks || 0).toLocaleString() }}</span></td>
            <td class="right"><span class="num">{{ Number(row.impressions || 0).toLocaleString() }}</span></td>
            <td class="right"><span class="num">{{ (Number(row.ctr || 0) * 100).toFixed(2) }}%</span></td>
          </tr>
        </tbody>
      </table>
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
import { comparePages, getSeoData } from '@/api/dashboardApi'
import { useSiteStore } from '@/stores/siteStore'
import { useFilterStore } from '@/stores/filterStore'

use([CanvasRenderer, LineChart, GridComponent, TooltipComponent, LegendComponent])

const COLORS = ['#6366f1', '#f59e0b', '#16a34a', '#dc2626', '#6b7280']

const siteStore = useSiteStore()
const filter = useFilterStore()
const loading = ref(false)
const pageOptions = ref([])
const selected = ref([])
const series = ref([])
const table = ref([])

const chartOption = computed(() => {
  const dateSet = new Set()
  series.value.forEach((s) => (s.trend || []).forEach((r) => dateSet.add(r.stat_date || r.statDate)))
  const dates = [...dateSet].sort()
  return {
    color: COLORS,
    legend: {
      data: series.value.map((s) => shortLabel(s.pageUrl)),
      top: 0,
      type: 'scroll',
      textStyle: { color: '#6b7280', fontSize: 11 },
    },
    tooltip: { trigger: 'axis', backgroundColor: '#fff', borderColor: '#e5e7eb', borderWidth: 1, textStyle: { color: '#111827', fontSize: 12 } },
    grid: { left: 8, right: 8, top: 36, bottom: 8, containLabel: true },
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
      axisLabel: { color: '#6b7280', fontSize: 11 },
    },
    series: series.value.map((s, idx) => {
      const map = Object.fromEntries((s.trend || []).map((r) => [r.stat_date || r.statDate, Number(r.clicks || 0)]))
      return {
        name: shortLabel(s.pageUrl),
        type: 'line',
        smooth: 0.15,
        showSymbol: false,
        lineStyle: { width: 2, color: COLORS[idx % COLORS.length] },
        data: dates.map((d) => map[d] ?? null),
      }
    }),
  }
})

function shortLabel(url) {
  try {
    const u = new URL(url)
    return u.pathname.length > 32 ? u.pathname.slice(0, 32) + '…' : u.pathname
  } catch {
    return url.length > 36 ? url.slice(0, 36) + '…' : url
  }
}

function onSelect(vals) {
  if (vals.length > 5) {
    selected.value = vals.slice(0, 5)
  }
  loadCompare()
}

async function loadOptions() {
  if (!siteStore.currentSiteId) return
  const res = await getSeoData(filter.apiParams({
    siteId: siteStore.currentSiteId,
    keywordLimit: 1,
    pageLimit: 100,
  }))
  pageOptions.value = (res.data?.pages || []).map((p) => p.page_url || p.pageUrl).filter(Boolean)
  if (!selected.value.length && pageOptions.value.length) {
    selected.value = pageOptions.value.slice(0, Math.min(3, pageOptions.value.length))
  }
  await loadCompare()
}

async function loadCompare() {
  if (!siteStore.currentSiteId || !selected.value.length) {
    series.value = []
    table.value = []
    return
  }
  loading.value = true
  try {
    const res = await comparePages(filter.apiParams({
      siteId: siteStore.currentSiteId,
      pageUrls: selected.value.join(','),
    }))
    series.value = res.data?.series || []
    table.value = res.data?.table || []
  } finally {
    loading.value = false
  }
}

watch(
  () => [siteStore.currentSiteId, filter.period, filter.platform, filter.customRange, filter.dateRange.start],
  () => loadOptions(),
  { immediate: true, deep: true }
)
</script>

<style scoped>
.picker { margin-bottom: 20px; }
.chart { height: 260px; width: 100%; }
</style>

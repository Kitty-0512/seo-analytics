<template>
  <div>
    <div class="subnav">
      <router-link to="/seo/keywords">Keywords</router-link>
      <router-link to="/seo/pages" class="active">Pages</router-link>
      <router-link to="/seo/query-page">Query–Page</router-link>
      <router-link to="/seo/opportunities">Opportunities</router-link>
    </div>

    <template v-if="loading">
      <div v-for="i in 8" :key="i" class="skeleton skeleton-line" style="height: 28px; margin-bottom: 8px" />
    </template>

    <template v-else>
      <table v-if="pages.length" class="plausible-table">
        <thead>
          <tr>
            <th @click="toggleSort('page')">Page</th>
            <th class="right" style="width: 100px" @click="toggleSort('clicks')">Clicks</th>
            <th class="right" style="width: 110px" @click="toggleSort('impressions')">Impressions</th>
            <th class="right" style="width: 80px" @click="toggleSort('ctr')">CTR</th>
            <th class="right" style="width: 120px">Share</th>
          </tr>
        </thead>
        <tbody>
          <template v-for="row in sorted" :key="row.page_url || row.pageUrl">
            <tr @click="toggleExpand(row)">
              <td>
                <div class="cell-pad">
                  <span class="expand">{{ expanded === pageKey(row) ? '▾' : '▸' }}</span>
                  {{ row.page_url || row.pageUrl }}
                </div>
              </td>
              <td class="right"><span class="num">{{ formatNum(row.clicks) }}</span></td>
              <td class="right"><span class="num">{{ formatNum(row.impressions) }}</span></td>
              <td class="right"><span class="num">{{ calcCtr(row) }}</span></td>
              <td class="right">
                <div class="bar-inline">
                  <span class="num">{{ sharePct(row.clicks) }}</span>
                  <div class="bar-track"><div class="bar-fill" :style="{ width: barWidth(row.clicks) }" /></div>
                </div>
              </td>
            </tr>
            <tr v-if="expanded === pageKey(row)" class="expand-row">
              <td colspan="5">
                <div class="expand-box">
                  <div class="nested-title">Related keywords</div>
                  <div v-if="detailLoading" class="skeleton skeleton-line" style="height: 24px" />
                  <table v-else-if="pageKeywords.length" class="plausible-table nested">
                    <thead>
                      <tr>
                        <th>Keyword</th>
                        <th class="right" style="width: 90px">Clicks</th>
                        <th class="right" style="width: 90px">Impr.</th>
                        <th class="right" style="width: 70px">Pos.</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr v-for="kw in pageKeywords" :key="kw.keyword">
                        <td><div class="cell-pad">{{ kw.keyword }}</div></td>
                        <td class="right"><span class="num">{{ formatNum(kw.clicks) }}</span></td>
                        <td class="right"><span class="num">{{ formatNum(kw.impressions) }}</span></td>
                        <td class="right">
                          <span class="num">{{ Number(kw.avg_position ?? kw.avgPosition ?? 0).toFixed(1) }}</span>
                        </td>
                      </tr>
                    </tbody>
                  </table>
                  <div v-else class="empty-state" style="padding: 12px 0">暂无数据</div>
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
import { getPageDetail, getSeoData } from '@/api/dashboardApi'
import { useSiteStore } from '@/stores/siteStore'
import { useFilterStore } from '@/stores/filterStore'

const siteStore = useSiteStore()
const filter = useFilterStore()
const loading = ref(false)
const pages = ref([])
const sortKey = ref('clicks')
const sortAsc = ref(false)
const expanded = ref(null)
const pageKeywords = ref([])
const detailLoading = ref(false)

const maxClicks = computed(() => Math.max(...pages.value.map((r) => Number(r.clicks || 0)), 1))

const sorted = computed(() => {
  const list = [...pages.value]
  list.sort((a, b) => {
    if (sortKey.value === 'page') {
      const av = String(a.page_url || a.pageUrl || '')
      const bv = String(b.page_url || b.pageUrl || '')
      return sortAsc.value ? av.localeCompare(bv) : bv.localeCompare(av)
    }
    if (sortKey.value === 'ctr') {
      return sortAsc.value ? ctrNum(a) - ctrNum(b) : ctrNum(b) - ctrNum(a)
    }
    const av = Number(a[sortKey.value] || 0)
    const bv = Number(b[sortKey.value] || 0)
    return sortAsc.value ? av - bv : bv - av
  })
  return list
})

function pageKey(row) { return row.page_url || row.pageUrl }
function formatNum(v) { return Number(v || 0).toLocaleString() }
function ctrNum(row) {
  const c = Number(row.clicks || 0)
  const i = Number(row.impressions || 0)
  return i ? c / i : 0
}
function calcCtr(row) { return `${(ctrNum(row) * 100).toFixed(2)}%` }
function barWidth(c) { return `${Math.max((Number(c || 0) / maxClicks.value) * 100, 2)}%` }
function sharePct(c) {
  const t = pages.value.reduce((s, r) => s + Number(r.clicks || 0), 0) || 1
  return `${((Number(c || 0) / t) * 100).toFixed(1)}%`
}
function toggleSort(key) {
  if (sortKey.value === key) sortAsc.value = !sortAsc.value
  else { sortKey.value = key; sortAsc.value = key === 'page' }
}

async function toggleExpand(row) {
  const key = pageKey(row)
  if (expanded.value === key) {
    expanded.value = null
    return
  }
  expanded.value = key
  detailLoading.value = true
  try {
    const res = await getPageDetail(filter.apiParams({
      siteId: siteStore.currentSiteId,
      pageUrl: key,
    }))
    pageKeywords.value = res.data?.keywords || []
  } finally {
    detailLoading.value = false
  }
}

async function load() {
  if (!siteStore.currentSiteId) return
  loading.value = true
  expanded.value = null
  try {
    const res = await getSeoData(filter.apiParams({
      siteId: siteStore.currentSiteId,
      keywordLimit: 1,
      pageLimit: 100,
    }))
    pages.value = res.data?.pages || []
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
.expand { display: inline-block; width: 14px; color: #6b7280; font-size: 10px; }
.expand-row { cursor: default; }
.expand-row:hover { background: transparent !important; }
.expand-box { padding: 8px 0 16px 18px; }
.nested-title { font-size: 12px; font-weight: 500; color: #6b7280; margin-bottom: 8px; }
.nested tbody tr { cursor: default; }
</style>

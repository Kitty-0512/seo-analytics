<template>
  <div>
    <div class="subnav">
      <router-link to="/seo/keywords">Keywords</router-link>
      <router-link to="/seo/pages">Pages</router-link>
      <router-link to="/seo/query-page" class="active">Query–Page</router-link>
      <router-link to="/seo/opportunities">Opportunities</router-link>
    </div>

    <div class="filters-row">
      <el-input
        v-model="keywordFilter"
        placeholder="Filter keyword"
        clearable
        size="small"
        style="max-width: 220px"
        @keyup.enter="load"
      />
      <el-input
        v-model="pageFilter"
        placeholder="Filter page URL"
        clearable
        size="small"
        style="max-width: 320px"
        @keyup.enter="load"
      />
      <el-button type="primary" size="small" :disabled="loading" @click="load">Apply</el-button>
    </div>

    <template v-if="loading">
      <div v-for="i in 6" :key="i" class="skeleton skeleton-line" style="height: 28px; margin-bottom: 8px" />
    </template>
    <template v-else>
      <table v-if="rows.length" class="plausible-table">
        <thead>
          <tr>
            <th>Keyword</th>
            <th>Page</th>
            <th class="right" style="width: 90px">Clicks</th>
            <th class="right" style="width: 110px">Impressions</th>
            <th class="right" style="width: 80px">Pos.</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(row, idx) in rows" :key="idx">
            <td>
              <button type="button" class="link-btn" @click="filterByKeyword(row.keyword)">
                {{ row.keyword }}
              </button>
            </td>
            <td>
              <button type="button" class="link-btn muted" @click="filterByPage(row.page_url || row.pageUrl)">
                {{ row.page_url || row.pageUrl }}
              </button>
            </td>
            <td class="right"><span class="num">{{ formatNum(row.clicks) }}</span></td>
            <td class="right"><span class="num">{{ formatNum(row.impressions) }}</span></td>
            <td class="right">
              <span class="num">{{ Number(row.avg_position ?? row.avgPosition ?? 0).toFixed(1) }}</span>
            </td>
          </tr>
        </tbody>
      </table>
      <div v-else class="empty-state">暂无 Query–Page 映射（请先对 GSC 同步）</div>
    </template>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { getQueryPage } from '@/api/dashboardApi'
import { useSiteStore } from '@/stores/siteStore'
import { useFilterStore } from '@/stores/filterStore'

const siteStore = useSiteStore()
const filter = useFilterStore()
const loading = ref(false)
const rows = ref([])
const keywordFilter = ref('')
const pageFilter = ref('')

function formatNum(v) {
  return Number(v || 0).toLocaleString()
}

function filterByKeyword(kw) {
  keywordFilter.value = kw || ''
  pageFilter.value = ''
  load()
}

function filterByPage(url) {
  pageFilter.value = url || ''
  keywordFilter.value = ''
  load()
}

async function load() {
  if (!siteStore.currentSiteId) {
    rows.value = []
    return
  }
  loading.value = true
  try {
    const params = filter.apiParams({
      siteId: siteStore.currentSiteId,
      limit: 100,
    })
    if (keywordFilter.value) params.keyword = keywordFilter.value
    if (pageFilter.value) params.pageUrl = pageFilter.value
    const res = await getQueryPage(params)
    rows.value = res.data || []
  } catch {
    rows.value = []
  } finally {
    loading.value = false
  }
}

watch(
  () => [siteStore.currentSiteId, filter.period, filter.platform, filter.customRange],
  () => load(),
  { immediate: true, deep: true },
)
</script>

<style scoped>
.subnav {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
  border-bottom: 1px solid #e5e7eb;
  padding-bottom: 8px;
}
.subnav a {
  color: #6b7280;
  text-decoration: none;
  font-size: 14px;
  padding-bottom: 8px;
}
.subnav a.active,
.subnav a.router-link-active {
  color: #111827;
  font-weight: 600;
  border-bottom: 2px solid #6366f1;
}
.filters-row {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}
.link-btn {
  appearance: none;
  border: 0;
  background: none;
  color: #4f46e5;
  cursor: pointer;
  font: inherit;
  text-align: left;
  padding: 0;
}
.link-btn.muted {
  color: #6b7280;
  word-break: break-all;
}
</style>

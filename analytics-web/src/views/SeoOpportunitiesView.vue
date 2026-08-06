<template>
  <div>
    <div class="subnav">
      <router-link to="/seo/keywords">Keywords</router-link>
      <router-link to="/seo/pages">Pages</router-link>
      <router-link to="/seo/query-page">Query–Page</router-link>
      <router-link to="/seo/opportunities" class="active">Opportunities</router-link>
    </div>

    <template v-if="loading">
      <div v-for="i in 6" :key="i" class="skeleton skeleton-line" style="height: 28px; margin-bottom: 8px" />
    </template>
    <template v-else>
      <div class="panel-block">
        <div class="panel-title">Striking distance (pos 4–20)</div>
        <table v-if="striking.length" class="plausible-table">
          <thead>
            <tr>
              <th>Keyword</th>
              <th class="right" style="width: 90px">Clicks</th>
              <th class="right" style="width: 110px">Impressions</th>
              <th class="right" style="width: 80px">Pos.</th>
              <th>Reason</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in striking" :key="'s-' + row.keyword">
              <td>{{ row.keyword }}</td>
              <td class="right"><span class="num">{{ formatNum(row.clicks) }}</span></td>
              <td class="right"><span class="num">{{ formatNum(row.impressions) }}</span></td>
              <td class="right"><span class="num">{{ Number(row.avgPosition || 0).toFixed(1) }}</span></td>
              <td class="muted">{{ row.reason }}</td>
            </tr>
          </tbody>
        </table>
        <div v-else class="empty-state">暂无 striking distance 机会</div>
      </div>

      <div class="panel-block">
        <div class="panel-title">Low CTR</div>
        <table v-if="lowCtr.length" class="plausible-table">
          <thead>
            <tr>
              <th>Keyword</th>
              <th class="right" style="width: 90px">Clicks</th>
              <th class="right" style="width: 110px">Impressions</th>
              <th class="right" style="width: 80px">CTR</th>
              <th class="right" style="width: 80px">Pos.</th>
              <th>Reason</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in lowCtr" :key="'c-' + row.keyword">
              <td>{{ row.keyword }}</td>
              <td class="right"><span class="num">{{ formatNum(row.clicks) }}</span></td>
              <td class="right"><span class="num">{{ formatNum(row.impressions) }}</span></td>
              <td class="right"><span class="num">{{ ((row.ctr || 0) * 100).toFixed(2) }}%</span></td>
              <td class="right"><span class="num">{{ Number(row.avgPosition || 0).toFixed(1) }}</span></td>
              <td class="muted">{{ row.reason }}</td>
            </tr>
          </tbody>
        </table>
        <div v-else class="empty-state">暂无 low CTR 机会</div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { getOpportunities } from '@/api/dashboardApi'
import { useSiteStore } from '@/stores/siteStore'
import { useFilterStore } from '@/stores/filterStore'

const siteStore = useSiteStore()
const filter = useFilterStore()
const loading = ref(false)
const striking = ref([])
const lowCtr = ref([])

function formatNum(v) {
  return Number(v || 0).toLocaleString()
}

async function load() {
  if (!siteStore.currentSiteId) {
    striking.value = []
    lowCtr.value = []
    return
  }
  loading.value = true
  try {
    const res = await getOpportunities(filter.apiParams({ siteId: siteStore.currentSiteId }))
    const data = res.data || {}
    striking.value = data.strikingDistance || []
    lowCtr.value = data.lowCtr || []
  } catch {
    striking.value = []
    lowCtr.value = []
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
.panel-block {
  margin-bottom: 28px;
}
.panel-title {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 12px;
}
.muted {
  color: #6b7280;
  font-size: 13px;
}
</style>

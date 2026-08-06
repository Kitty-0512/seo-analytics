<template>
  <div>
    <template v-if="loading">
      <div v-for="i in 5" :key="i" class="skeleton skeleton-line" style="height: 48px; margin-bottom: 8px" />
    </template>
    <template v-else>
      <div v-if="alerts.length" class="alert-list">
        <div v-for="(a, idx) in alerts" :key="idx" class="alert-item">
          <div class="alert-icon" :class="a.type">
            {{ a.type === 'position_drop' ? '↓' : '!' }}
          </div>
          <div class="alert-body">
            <div class="alert-target">{{ a.target }}</div>
            <div class="alert-reason">{{ a.reason }}</div>
          </div>
          <div class="alert-date">{{ a.date }}</div>
        </div>
      </div>
      <div v-else class="empty-state">暂无异常</div>
    </template>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { getAlerts } from '@/api/dashboardApi'
import { useSiteStore } from '@/stores/siteStore'
import { useFilterStore } from '@/stores/filterStore'

const siteStore = useSiteStore()
const filter = useFilterStore()
const loading = ref(false)
const alerts = ref([])

async function load() {
  if (!siteStore.currentSiteId) return
  loading.value = true
  try {
    const res = await getAlerts(filter.apiParams({ siteId: siteStore.currentSiteId }))
    alerts.value = res.data || []
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
.alert-list { border-top: 1px solid #e5e7eb; }

.alert-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 14px 0;
  border-bottom: 1px solid #f3f4f6;
}

.alert-icon {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 600;
  flex-shrink: 0;
  background: #fef2f2;
  color: #dc2626;
}

.alert-icon.traffic_drop {
  background: #fff7ed;
  color: #f59e0b;
}

.alert-target {
  font-size: 14px;
  font-weight: 500;
  color: #111827;
}

.alert-reason {
  font-size: 13px;
  color: #6b7280;
  margin-top: 2px;
}

.alert-date {
  margin-left: auto;
  font-size: 12px;
  color: #6b7280;
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}
</style>

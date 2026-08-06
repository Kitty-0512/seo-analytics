<template>
  <div class="filter-bar">
    <div class="filter-inner">
      <div class="filter-left">
        <div class="pill-group">
          <button
            v-for="p in periodOptions"
            :key="p.key"
            type="button"
            class="pill-btn"
            :class="{ active: filter.period === p.key }"
            @click="onPeriod(p.key)"
          >
            {{ p.label }}
          </button>
        </div>
        <el-date-picker
          v-if="filter.period === 'custom'"
          :model-value="filter.customRange"
          type="daterange"
          range-separator="–"
          start-placeholder="Start"
          end-placeholder="End"
          value-format="YYYY-MM-DD"
          size="small"
          @update:model-value="filter.setCustomRange"
        />
        <slot name="left-extra" />
      </div>
      <div class="filter-right">
        <slot name="right-extra" />
        <div v-if="showPlatform" class="pill-group">
          <button
            v-for="p in platformOptions"
            :key="p.key"
            type="button"
            class="pill-btn"
            :class="{ active: filter.platform === p.key }"
            @click="filter.setPlatform(p.key)"
          >
            {{ p.label }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useFilterStore } from '@/stores/filterStore'

defineProps({
  showPlatform: { type: Boolean, default: true },
})

const filter = useFilterStore()

const periodOptions = [
  { key: '7d', label: '7天' },
  { key: '30d', label: '30天' },
  { key: 'custom', label: '自定义' },
]

const platformOptions = [
  { key: '', label: 'All' },
  { key: 'GSC', label: 'GSC' },
  { key: 'BING', label: 'Bing' },
]

function onPeriod(key) {
  filter.setPeriod(key)
}
</script>

<style scoped>
.filter-bar {
  border-bottom: 1px solid #e5e7eb;
  background: #ffffff;
}

.filter-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 12px 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.filter-left,
.filter-right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
</style>

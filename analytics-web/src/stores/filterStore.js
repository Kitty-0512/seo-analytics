import { defineStore } from 'pinia'
import { computed, ref } from 'vue'

function fmt(d) {
  return d.toISOString().slice(0, 10)
}

export const useFilterStore = defineStore('filter', () => {
  const period = ref('30d')
  const platform = ref('')
  const customRange = ref(null)

  const dateRange = computed(() => {
    if (period.value === 'custom' && customRange.value?.length === 2) {
      return { start: customRange.value[0], end: customRange.value[1] }
    }
    const end = new Date()
    end.setDate(end.getDate() - 1)
    const start = new Date(end)
    start.setDate(end.getDate() - (period.value === '7d' ? 6 : 29))
    return { start: fmt(start), end: fmt(end) }
  })

  const previousRange = computed(() => {
    const { start, end } = dateRange.value
    const s = new Date(start)
    const e = new Date(end)
    const days = Math.round((e - s) / 86400000) + 1
    const prevEnd = new Date(s)
    prevEnd.setDate(prevEnd.getDate() - 1)
    const prevStart = new Date(prevEnd)
    prevStart.setDate(prevEnd.getDate() - (days - 1))
    return { start: fmt(prevStart), end: fmt(prevEnd) }
  })

  function setPeriod(key) {
    period.value = key
  }

  function setPlatform(key) {
    platform.value = key
  }

  function setCustomRange(range) {
    customRange.value = range
    if (range?.length === 2) period.value = 'custom'
  }

  function apiParams(extra = {}) {
    const { start, end } = dateRange.value
    return {
      startDate: start,
      endDate: end,
      platform: platform.value || undefined,
      ...extra,
    }
  }

  return {
    period,
    platform,
    customRange,
    dateRange,
    previousRange,
    setPeriod,
    setPlatform,
    setCustomRange,
    apiParams,
  }
})

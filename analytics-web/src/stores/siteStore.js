import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { listSites } from '@/api/siteApi'

export const useSiteStore = defineStore('site', () => {
  const sites = ref([])
  const currentSiteId = ref(
    localStorage.getItem('seo_current_site_id')
      ? Number(localStorage.getItem('seo_current_site_id'))
      : null
  )

  const currentSite = computed(() =>
    sites.value.find((s) => s.id === currentSiteId.value) || null
  )

  function setCurrentSiteId(id) {
    currentSiteId.value = id
    if (id != null) {
      localStorage.setItem('seo_current_site_id', String(id))
    } else {
      localStorage.removeItem('seo_current_site_id')
    }
  }

  async function loadSites() {
    const res = await listSites()
    sites.value = res.data || []
    if (!currentSiteId.value && sites.value.length > 0) {
      setCurrentSiteId(sites.value[0].id)
    }
    if (
      currentSiteId.value &&
      sites.value.length > 0 &&
      !sites.value.find((s) => s.id === currentSiteId.value)
    ) {
      setCurrentSiteId(sites.value[0].id)
    }
    return sites.value
  }

  return {
    sites,
    currentSiteId,
    currentSite,
    setCurrentSiteId,
    loadSites,
  }
})

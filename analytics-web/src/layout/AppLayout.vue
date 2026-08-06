<template>
  <div class="app-shell">
    <header class="topnav">
      <div class="topnav-inner">
        <div class="topnav-left">
          <router-link to="/dashboard" class="logo">
            <span class="logo-mark" />
            <span class="logo-text">SEO Analytics</span>
          </router-link>
          <nav class="nav-links">
            <router-link
              v-for="item in menuItems"
              :key="item.path"
              :to="item.path"
              class="nav-link"
              :class="{ active: isActive(item) }"
            >
              {{ item.title }}
            </router-link>
          </nav>
        </div>
        <div class="topnav-right">
          <el-select
            v-model="siteId"
            placeholder="Select site"
            style="width: 180px"
            size="small"
            filterable
            @change="onSiteChange"
          >
            <el-option
              v-for="s in siteStore.sites"
              :key="s.id"
              :label="s.name"
              :value="s.id"
            />
          </el-select>
        </div>
      </div>
    </header>

    <FilterBar v-if="showFilterBar" :show-platform="showPlatformFilter" />

    <main class="content-wrap">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useSiteStore } from '@/stores/siteStore'
import FilterBar from '@/components/FilterBar.vue'

const route = useRoute()
const siteStore = useSiteStore()
const siteId = ref(null)

const menuItems = [
  { path: '/dashboard', title: 'Dashboard', match: ['/dashboard'] },
  { path: '/seo/keywords', title: 'SEO', match: ['/seo'] },
  { path: '/compare/period', title: 'Compare', match: ['/compare'] },
  { path: '/ai', title: 'AI', match: ['/ai'] },
  { path: '/alerts', title: 'Alerts', match: ['/alerts'] },
  { path: '/settings', title: 'Settings', match: ['/settings'] },
]

const showFilterBar = computed(() => !['/settings', '/ai'].includes(route.path))
const showPlatformFilter = computed(() => !route.path.startsWith('/compare/platform'))

function isActive(item) {
  return item.match.some((p) => route.path === p || route.path.startsWith(p + '/'))
}

function onSiteChange(id) {
  siteStore.setCurrentSiteId(id)
}

watch(
  () => siteStore.currentSiteId,
  (id) => { siteId.value = id },
  { immediate: true }
)

onMounted(async () => {
  try {
    await siteStore.loadSites()
    siteId.value = siteStore.currentSiteId
  } catch { /* ignore */ }
})
</script>

<style scoped>
.app-shell {
  min-height: 100%;
  background: #ffffff;
}

.topnav {
  height: 56px;
  border-bottom: 1px solid #e5e7eb;
  background: #ffffff;
  position: sticky;
  top: 0;
  z-index: 50;
}

.topnav-inner {
  max-width: 1200px;
  margin: 0 auto;
  height: 100%;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.topnav-left {
  display: flex;
  align-items: center;
  min-width: 0;
}

.logo {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.logo-mark {
  width: 16px;
  height: 16px;
  border-radius: 6px;
  background: #6366f1;
}

.logo-text {
  font-size: 14px;
  font-weight: 600;
  letter-spacing: -0.02em;
  color: #111827;
}

.nav-links {
  display: flex;
  align-items: center;
  gap: 4px;
  height: 56px;
  margin-left: 48px;
}

.nav-link {
  display: inline-flex;
  align-items: center;
  height: 100%;
  padding: 0 12px;
  font-size: 14px;
  font-weight: 500;
  color: #6b7280;
  border-bottom: 2px solid transparent;
}

.nav-link:hover { color: #111827; }

.nav-link.active {
  color: #6366f1;
  border-bottom-color: #6366f1;
}

.topnav-right { flex-shrink: 0; }

@media (max-width: 860px) {
  .nav-links { margin-left: 24px; }
  .logo-text { display: none; }
}
</style>

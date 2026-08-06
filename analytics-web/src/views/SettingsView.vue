<template>
  <div class="settings-page">
    <div class="grid">
      <div class="panel">
        <div class="panel-header">
          <span class="panel-title">Sites</span>
          <el-button type="primary" size="small" @click="openCreate">Add site</el-button>
        </div>
        <table v-if="siteStore.sites.length" class="plausible-table">
          <thead>
            <tr>
              <th>Name</th>
              <th>Domain</th>
              <th class="right" style="width: 120px">Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in siteStore.sites" :key="row.id">
              <td>
                <span class="cell-label" :class="{ current: row.id === siteStore.currentSiteId }">
                  {{ row.name }}
                </span>
              </td>
              <td><span class="muted">{{ row.domain }}</span></td>
              <td class="right">
                <button type="button" class="link-btn" @click="selectSite(row.id)">Use</button>
                <button type="button" class="link-btn danger" @click="removeSite(row.id)">Delete</button>
              </td>
            </tr>
          </tbody>
        </table>
        <div v-else class="empty-state">暂无数据</div>
      </div>

      <div>
        <div class="panel">
          <div class="panel-title">Platform auth</div>
          <p v-if="!siteStore.currentSiteId" class="empty-state" style="padding: 0 0 16px; text-align: left">
            请先选择站点
          </p>
          <div class="auth-row">
            <div>
              <div class="auth-name">Google Search Console</div>
              <div class="hint">OAuth2</div>
            </div>
            <el-button
              type="primary"
              plain
              size="small"
              :disabled="!siteStore.currentSiteId"
              @click="connectGsc"
            >
              Connect
            </el-button>
          </div>
          <div class="divider" />
          <div class="auth-row">
            <div>
              <div class="auth-name">Bing Webmaster</div>
              <div class="hint">API key</div>
            </div>
          </div>
          <el-input
            v-model="bingApiKey"
            placeholder="Enter Bing API key"
            size="small"
            style="margin: 12px 0"
            :disabled="!siteStore.currentSiteId"
          />
          <el-button
            type="primary"
            plain
            size="small"
            :disabled="!siteStore.currentSiteId"
            @click="saveBing"
          >
            Save key
          </el-button>
          <div v-if="authList.length" class="auth-status">
            <div v-for="a in authList" :key="a.id" class="status-item">
              {{ a.platform }} connected
            </div>
          </div>
        </div>

        <div class="panel">
          <div class="panel-title">Manual sync</div>
          <div class="sync-meta" v-if="lastSyncLabel">Last sync: {{ lastSyncLabel }}</div>
          <div class="sync-row">
            <span class="field-label">Platform</span>
            <div class="pill-group">
              <button
                v-for="p in syncPlatforms"
                :key="p.key"
                type="button"
                class="pill-btn"
                :class="{ active: syncPlatform === p.key }"
                @click="syncPlatform = p.key"
              >
                {{ p.label }}
              </button>
            </div>
          </div>
          <div class="sync-row">
            <span class="field-label">Date range</span>
            <el-date-picker
              v-model="syncRange"
              type="daterange"
              range-separator="–"
              start-placeholder="Start"
              end-placeholder="End"
              value-format="YYYY-MM-DD"
              size="small"
            />
          </div>
          <el-button
            type="primary"
            size="small"
            :disabled="!siteStore.currentSiteId || syncing"
            @click="runSync"
          >
            {{ syncing ? 'Syncing…' : 'Sync now' }}
          </el-button>
        </div>
      </div>
    </div>

    <el-dialog v-model="dialogVisible" title="Add site" width="440px">
      <el-form :model="form" label-position="top">
        <el-form-item label="Name" required>
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="Domain" required>
          <el-input v-model="form.domain" placeholder="https://example.com" />
        </el-form-item>
        <el-form-item label="GSC property">
          <el-input v-model="form.gscProperty" placeholder="sc-domain:example.com" />
        </el-form-item>
        <el-form-item label="Bing site URL">
          <el-input v-model="form.bingSiteUrl" placeholder="https://example.com" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">Cancel</el-button>
        <el-button type="primary" :disabled="saving" @click="saveSite">
          {{ saving ? 'Saving…' : 'Save' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useSiteStore } from '@/stores/siteStore'
import { createSite, deleteSite } from '@/api/siteApi'
import { getAuthStatus, getGscAuthorizeUrl, saveBingApiKey } from '@/api/authApi'
import { syncData, getSyncStatus } from '@/api/syncApi'

const siteStore = useSiteStore()
const dialogVisible = ref(false)
const saving = ref(false)
const syncing = ref(false)
const bingApiKey = ref('')
const authList = ref([])
const syncPlatform = ref('')
const syncRange = ref(null)
const lastSyncLabel = ref('')
const form = ref({
  name: '',
  domain: '',
  gscProperty: '',
  bingSiteUrl: '',
})

const syncPlatforms = [
  { key: '', label: 'All' },
  { key: 'GSC', label: 'GSC' },
  { key: 'BING', label: 'Bing' },
]

function openCreate() {
  form.value = { name: '', domain: '', gscProperty: '', bingSiteUrl: '' }
  dialogVisible.value = true
}

function selectSite(id) {
  siteStore.setCurrentSiteId(id)
  ElMessage.success('Site selected')
}

async function saveSite() {
  if (!form.value.name || !form.value.domain) {
    ElMessage.warning('Name and domain are required')
    return
  }
  saving.value = true
  try {
    await createSite(form.value)
    await siteStore.loadSites()
    dialogVisible.value = false
    ElMessage.success('Site created')
  } finally {
    saving.value = false
  }
}

async function removeSite(id) {
  await ElMessageBox.confirm('Delete this site?', 'Confirm', { type: 'warning' })
  await deleteSite(id)
  await siteStore.loadSites()
  ElMessage.success('Deleted')
}

async function connectGsc() {
  const res = await getGscAuthorizeUrl(siteStore.currentSiteId)
  const url = res.data?.authorizeUrl
  if (url) window.location.href = url
}

async function saveBing() {
  if (!bingApiKey.value) {
    ElMessage.warning('API key required')
    return
  }
  await saveBingApiKey({
    siteId: siteStore.currentSiteId,
    apiKey: bingApiKey.value,
  })
  ElMessage.success('Bing API key saved')
  await loadAuth()
}

async function loadAuth() {
  if (!siteStore.currentSiteId) {
    authList.value = []
    return
  }
  try {
    const res = await getAuthStatus(siteStore.currentSiteId)
    authList.value = res.data || []
  } catch {
    authList.value = []
  }
}

async function loadSyncStatus() {
  if (!siteStore.currentSiteId) {
    lastSyncLabel.value = ''
    return
  }
  try {
    const res = await getSyncStatus({ siteId: siteStore.currentSiteId })
    const data = res.data || {}
    lastSyncLabel.value = data.lastSyncAt || data.lastSync || ''
  } catch {
    lastSyncLabel.value = ''
  }
}

async function runSync() {
  syncing.value = true
  try {
    const params = { siteId: siteStore.currentSiteId }
    if (syncPlatform.value) params.platform = syncPlatform.value
    if (syncRange.value?.length === 2) {
      params.startDate = syncRange.value[0]
      params.endDate = syncRange.value[1]
    }
    await syncData(params)
    ElMessage.success('Sync completed')
    await loadSyncStatus()
  } finally {
    syncing.value = false
  }
}

watch(
  () => siteStore.currentSiteId,
  () => {
    loadAuth()
    loadSyncStatus()
  },
  { immediate: true }
)

onMounted(() => {
  siteStore.loadSites()
})
</script>

<style scoped>
.grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-4);
  align-items: start;
}

.muted {
  color: var(--color-muted);
}

.current {
  color: var(--color-primary);
  font-weight: 500;
}

.link-btn {
  appearance: none;
  border: none;
  background: none;
  color: var(--color-primary);
  font: inherit;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  padding: 0 0 0 12px;
}

.link-btn.danger {
  color: var(--color-negative);
}

.auth-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.auth-name {
  font-size: 14px;
  font-weight: 500;
}

.divider {
  height: 1px;
  background: var(--color-border);
  margin: 16px 0;
}

.auth-status {
  margin-top: var(--space-4);
}

.status-item {
  font-size: 13px;
  color: var(--color-positive);
  font-weight: 500;
  margin-bottom: 4px;
}

.sync-row {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  margin-bottom: var(--space-4);
}

.sync-meta {
  font-size: 13px;
  color: var(--color-muted);
  margin-bottom: var(--space-4);
}

.field-label {
  width: 80px;
  flex-shrink: 0;
  font-size: 13px;
  color: var(--color-muted);
}

@media (max-width: 900px) {
  .grid {
    grid-template-columns: 1fr;
  }
}
</style>

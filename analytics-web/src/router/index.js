import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/', redirect: '/dashboard' },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: () => import('../views/DashboardView.vue'),
    meta: { title: 'Dashboard' },
  },
  { path: '/seo', redirect: '/seo/keywords' },
  {
    path: '/seo/keywords',
    name: 'SeoKeywords',
    component: () => import('../views/SeoKeywordsView.vue'),
    meta: { title: 'Keywords' },
  },
  {
    path: '/seo/pages',
    name: 'SeoPages',
    component: () => import('../views/SeoPagesView.vue'),
    meta: { title: 'Pages' },
  },
  {
    path: '/seo/query-page',
    name: 'SeoQueryPage',
    component: () => import('../views/SeoQueryPageView.vue'),
    meta: { title: 'Query–Page' },
  },
  {
    path: '/seo/opportunities',
    name: 'SeoOpportunities',
    component: () => import('../views/SeoOpportunitiesView.vue'),
    meta: { title: 'Opportunities' },
  },
  { path: '/compare', redirect: '/compare/period' },
  {
    path: '/compare/period',
    name: 'ComparePeriod',
    component: () => import('../views/ComparePeriodView.vue'),
    meta: { title: 'Compare Period' },
  },
  {
    path: '/compare/platform',
    name: 'ComparePlatform',
    component: () => import('../views/ComparePlatformView.vue'),
    meta: { title: 'Compare Platform' },
  },
  {
    path: '/compare/pages',
    name: 'ComparePages',
    component: () => import('../views/ComparePagesView.vue'),
    meta: { title: 'Compare Pages' },
  },
  {
    path: '/ai',
    name: 'AI',
    component: () => import('../views/AiView.vue'),
    meta: { title: 'AI' },
  },
  {
    path: '/alerts',
    name: 'Alerts',
    component: () => import('../views/AlertsView.vue'),
    meta: { title: 'Alerts' },
  },
  {
    path: '/settings',
    name: 'Settings',
    component: () => import('../views/SettingsView.vue'),
    meta: { title: 'Settings' },
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

export default router

import request from '@/utils/request'

export function getDashboard(params) {
  return request.get('/dashboard', { params })
}

export function getSeoData(params) {
  return request.get('/seo', { params })
}

export function getKeywordTrend(params) {
  return request.get('/seo/keyword-trend', { params })
}

export function getPageDetail(params) {
  return request.get('/seo/page-detail', { params })
}

export function getQueryPage(params) {
  return request.get('/seo/query-page', { params })
}

export function getOpportunities(params) {
  return request.get('/seo/opportunities', { params })
}

export function comparePlatform(params) {
  return request.get('/compare/platform', { params })
}

export function comparePages(params) {
  return request.get('/compare/pages', { params })
}

export function getAlerts(params) {
  return request.get('/alerts', { params })
}

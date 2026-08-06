import request from '@/utils/request'

export function getGscAuthorizeUrl(siteId) {
  return request.get('/auth/gsc/authorize', { params: { siteId } })
}

export function saveBingApiKey(data) {
  return request.post('/auth/bing', data)
}

export function getAuthStatus(siteId) {
  return request.get('/auth/status', { params: { siteId } })
}

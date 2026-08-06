import request from '@/utils/request'

export function syncData(params) {
  return request.post('/sync', null, { params })
}

export function getSyncStatus(params) {
  return request.get('/sync/status', { params })
}

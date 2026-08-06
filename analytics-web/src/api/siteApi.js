import request from '@/utils/request'

export function listSites() {
  return request.get('/sites')
}

export function createSite(data) {
  return request.post('/sites', data)
}

export function updateSite(id, data) {
  return request.put(`/sites/${id}`, data)
}

export function deleteSite(id) {
  return request.delete(`/sites/${id}`)
}

import request from '@/utils/request'

export function aiChat(data) {
  return request.post('/ai/chat', data)
}

export function aiHistory(params) {
  return request.get('/ai/history', { params })
}

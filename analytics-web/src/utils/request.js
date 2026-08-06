import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  // Local nginx proxies /api; on Render set VITE_API_BASE_URL to the API public URL + /api
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 60000,
})

request.interceptors.response.use(
  (response) => {
    const payload = response.data
    if (payload && typeof payload.code === 'number' && payload.code !== 0) {
      ElMessage.error(payload.message || 'Request failed')
      return Promise.reject(new Error(payload.message || 'Request failed'))
    }
    return payload
  },
  (error) => {
    const msg = error.response?.data?.message || error.message || 'Network error'
    ElMessage.error(msg)
    return Promise.reject(error)
  }
)

export default request

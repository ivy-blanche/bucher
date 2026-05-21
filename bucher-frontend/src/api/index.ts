import axios, { type AxiosInstance, type AxiosRequestConfig, type AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import JSONbig from 'json-bigint'

const instance: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  },
  transformResponse: [
    function (data: unknown) {
      if (typeof data === 'string' && data) {
        try {
          return JSONbig({ storeAsString: true }).parse(data)
        } catch {
          return data
        }
      }
      return data
    },
  ]
})

// 请求拦截器
instance.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器
instance.interceptors.response.use(
  (response: AxiosResponse) => {
    const { data, config } = response
    // blob / arraybuffer 响应直接透传原始 response（不解析 JSON）
    if (config.responseType === 'blob' || config.responseType === 'arraybuffer') {
      return response
    }
    if (data.code === 200) {
      return data.data
    }
    ElMessage.error(data.message || '请求失败')
    return Promise.reject(new Error(data.message || '请求失败'))
  },
  (error) => {
    const message = error.response?.data?.message || error.message || '网络错误'
    ElMessage.error(message)

    // 401 未授权，跳转登录页（排除公开接口及 admin 接口）
    if (error.response?.status === 401) {
      const url = error.config?.url || ''
      const publicApis = ['/auth/login', '/auth/register', '/auth/send-code', '/auth/reset-password']
      const isPublicApi = publicApis.some(api => url.includes(api))
      const isAdminApi = url.includes('/admin/')

      if (!isPublicApi && !isAdminApi) {
        localStorage.removeItem('token')
        localStorage.removeItem('userInfo')
        window.location.href = '/login'
      }
    }

    return Promise.reject(error)
  }
)

// 封装请求方法
const request = {
  get<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
    return instance.get(url, config)
  },
  post<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
    return instance.post(url, data, config)
  },
  put<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
    return instance.put(url, data, config)
  },
  delete<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
    return instance.delete(url, config)
  }
}

export default request

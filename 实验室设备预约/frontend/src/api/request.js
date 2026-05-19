import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'
import { useUserStore } from '../stores/user'

/**
 * 网络层兜底错误提示（断网、超时之类的）
 * 业务错误（400/404/500）交给各页面自己处理，这里不重复弹
 */
function showErrorToast (message) {
  ElMessage.error({ message: message || '网络错误', grouping: true })
}

// 错误去重，同样的消息3秒内只弹一次
let lastErrorTime = 0
let lastErrorMsg = ''
const ERROR_DEBOUNCE_MS = 3000

function safeShowError(msg) {
  const now = Date.now()
  if (msg === lastErrorMsg && now - lastErrorTime < ERROR_DEBOUNCE_MS) {
    return
  }
  lastErrorTime = now
  lastErrorMsg = msg
  showErrorToast(msg)
}

// 创建axios实例
const request = axios.create({
  baseURL: '/api',
  timeout: 60000
})

// 导出等耗时操作用的，超时设长一点
const longTimeoutRequest = axios.create({
  baseURL: '/api',
  timeout: 120000
})

let requestInterceptorId = null
let responseInterceptorId = null

// 请求拦截器，防止重复注册
function ensureInterceptor () {
  if (requestInterceptorId !== null) return
  requestInterceptorId = request.interceptors.request.use(
    config => {
      const userStore = useUserStore()
      if (userStore.token) {
        config.headers['Authorization'] = 'Bearer ' + userStore.token
      }
      return config
    },
    error => Promise.reject(error)
  )
}

// 响应拦截器，防止重复注册
function ensureResponseInterceptor () {
  if (responseInterceptorId !== null) return
  responseInterceptorId = request.interceptors.response.use(
    response => {
      const cfg = response.config || {}
      if (cfg.responseType === 'blob') {
        return response.data
      }
      const res = response.data
      if ((res.code !== undefined && res.code !== 200) ||
          (res.success !== undefined && res.success === false)) {
        const msg = res.message || '请求失败'
        const err = Object.assign(new Error(msg), { _fromResponse: true })
        err.response = response
        return Promise.reject(err)
      }
      return res
    },
    error => {
      const cfg = error.config || {}
      if (cfg.skipErrorNotify) {
        return Promise.reject(error)
      }
      if (!error.response) {
        safeShowError(error.message || '网络错误，请检查网络连接')
        return Promise.reject(error)
      }
      const data = error.response?.data
      let text = ''
      if (typeof data === 'string' && data.trim() !== '') {
        text = data.trim()
      } else if (data != null && typeof data === 'object' && data.message != null && data.message !== '') {
        text = typeof data.message === 'string' ? data.message : String(data.message)
      }
      // 400/404/500 这些业务错误交给页面自己提示，拦截器不重复弹
      // 403 需要显示（比如用户被禁用）
      const status = error.response?.status
      const shouldNotifyInInterceptor = !cfg.skipErrorNotify && ![400, 404, 500].includes(status)
      if (text && shouldNotifyInInterceptor) {
        safeShowError(text)
      }
      // 把400/403的具体错误信息挂到error.message上，方便页面catch里取
      if ((status === 400 || status === 403) && text) {
        error.message = text;
      }
      if (error.response.status === 401) {
        router.push('/login')
      }
      return Promise.reject(error)
    }
  )
}

ensureInterceptor()
ensureResponseInterceptor()

export { longTimeoutRequest }
export default request

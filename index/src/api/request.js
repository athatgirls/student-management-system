import axios from 'axios'
import { ElMessage } from 'element-plus'
import { isPreviewMode } from '@/utils/previewMode'
import { previewAdapter } from '@/mock/previewData'

// Use a same-origin path by default. In production Nginx proxies this path to
// the backend; in development vue.config.js provides the same proxy. This
// avoids shipping a bundle that points every visitor to their own localhost.
const API_BASE_URL = process.env.VUE_APP_API_BASE_URL || '/SCSE@hbut/msi'

// 创建axios实例
const request = axios.create({
    baseURL: API_BASE_URL,
    timeout: 15000,
    headers: {
        'Content-Type': 'application/json'
    }
})

// 请求拦截器
request.interceptors.request.use(
    config => {
        if (isPreviewMode()) {
            config.adapter = previewAdapter
        }

        // 可以在这里添加token等认证信息
        const token = localStorage.getItem('token')
        if (token) {
            config.headers.Authorization = `Bearer ${token}`
        }
        return config
    },
    error => {
        console.error('请求错误:', error)
        return Promise.reject(error)
    }
)

// 响应拦截器
request.interceptors.response.use(
    response => {
        // 只返回后端data，业务判断交给页面
        return response.data
    },
    error => {
        console.error('响应错误:', error)
        if (error.response) {
            const { status, data } = error.response
            switch (status) {
                case 401:
                    ElMessage.error('未授权，请重新登录')
                    break
                case 403:
                    ElMessage.error('拒绝访问')
                    break
                case 404:
                    ElMessage.error('请求的资源不存在')
                    break
                case 500:
                    ElMessage.error('服务器内部错误')
                    break
                case 502:
                case 503:
                case 504:
                    ElMessage.error(`服务暂时不可用（${status}），请稍后重试；持续失败请联系管理员检查网关和后端`)
                    break
                case 429:
                    ElMessage.error('操作过于频繁，请稍后重试')
                    break
                default:
                    ElMessage.error(data?.message || '网络错误')
            }
        } else if (error.request) {
            ElMessage.error(error.code === 'ECONNABORTED'
                ? '请求超时，请稍后重试'
                : '未收到服务器响应，请检查网络或代理设置；持续失败请联系管理员')
        } else {
            ElMessage.error('请求配置错误')
        }
        return Promise.reject(error)
    }
)

export default request

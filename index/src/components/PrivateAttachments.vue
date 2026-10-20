<template>
  <div class="attachment-container">
    <div v-for="(file, index) in files" :key="file.url || index" class="attachment-item">
      <el-button link type="primary" @click="openFile(file)">{{ file.name || '附件' }}（{{ isPreviewable(file.url) ? '新页面查看' : '下载' }}）</el-button>
    </div>
    <span v-if="!files.length">暂无有效附件</span>
  </div>
</template>
<script setup>
import { computed, onUnmounted, defineProps } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/api/request'
import { getFileUrl } from '@/utils/imageUrl'
import { privateUploadUrl } from '@/utils/privateUploadUrl'

const props = defineProps({ attachments: { type: [String, Array, Object], default: '' } })
const files = computed(() => {
  try {
    const entries = typeof props.attachments === 'string' ? JSON.parse(props.attachments || '[]') : props.attachments
    return (Array.isArray(entries) ? entries : [entries]).map(file => typeof file === 'string' ? { url: file, name: file.split('/').pop() } : file)
      .filter(file => file && typeof file.url === 'string')
  } catch {
    const value = props.attachments
    return typeof value === 'string' && value.trim() ? [{ url: value, name: value.split('/').pop() }] : []
  }
})
const isPreviewable = url => /\.(jpg|jpeg|png|pdf)$/i.test(String(url || '').split(/[?#]/)[0])
const controllers = []
let active = true

const fetchFile = async rawUrl => {
  const url = privateUploadUrl(getFileUrl(rawUrl), window.location.origin, request.defaults.baseURL)
  if (!url) throw new Error('附件地址无效')
  const token = localStorage.getItem('token')
  if (!token) throw new Error('请重新登录后查看附件')
  const controller = new AbortController()
  controllers.push(controller)
  const response = await fetch(url, { headers: { Authorization: `Bearer ${token}` }, signal: controller.signal, credentials: 'same-origin' })
  if (!response.ok) throw new Error(response.status === 404 ? '附件不存在' : '附件加载失败，请检查登录状态或访问权限')
  return response.blob()
}

onUnmounted(() => {
  active = false
  controllers.forEach(controller => controller.abort())
})

const openFile = async file => {
  if (!isPreviewable(file.url)) return download(file)
  // Open synchronously on the click event so browsers do not block the new tab while fetching.
  const tab = window.open('', '_blank')
  if (!tab) return ElMessage.warning('请允许浏览器打开新标签页后重试')
  tab.opener = null
  try {
    const content = await fetchFile(file.url)
    if (!active) return tab.close()
    // The private endpoint serves PDF as application/octet-stream for downloads.
    // Re-wrap with a previewable MIME type without weakening server-side authorization.
    const mime = /\.pdf$/i.test(file.url) ? 'application/pdf' : /\.png$/i.test(file.url) ? 'image/png' : 'image/jpeg'
    const url = URL.createObjectURL(new Blob([content], { type: mime }))
    tab.location.replace(url)
    const interval = window.setInterval(() => {
      if (tab.closed) {
        window.clearInterval(interval)
        URL.revokeObjectURL(url)
      }
    }, 1000)
  } catch (error) {
    tab.close()
    if (error.name !== 'AbortError') ElMessage.error(error.message || '查看附件失败')
  }
}

const download = async file => {
  try {
    const blob = await fetchFile(file.url)
    if (!active) return
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = file.name || String(file.url).split('/').pop()
    document.body.appendChild(link)
    link.click()
    link.remove()
    // Keep the blob URL alive until the browser starts the download.
    setTimeout(() => URL.revokeObjectURL(url), 1000)
  } catch (error) {
    if (error.name !== 'AbortError') ElMessage.error(error.message || '下载附件失败')
  }
}
</script>
<style scoped>
.attachment-container { display: flex; flex-wrap: wrap; gap: 12px; align-items: center; }
</style>

/** 统一把后端上传路径转换为同源 URL，开发和生产都交给代理转发。 */
const CONTEXT_PATH = (process.env.VUE_APP_CONTEXT_PATH || '/SCSE@hbut').replace(/\/$/, '')

export const getImageUrl = (url) => {
  if (!url) return ''

  const value = String(url).trim()
  if (!value) return ''

  if (/^https?:\/\//i.test(value)) {
    try {
      const parsed = new URL(value)
      if (['localhost', '127.0.0.1'].includes(parsed.hostname)) {
        return `${parsed.pathname}${parsed.search}${parsed.hash}`
      }
    } catch {
      return value
    }
    return value
  }

  const contextIndex = value.indexOf('SCSE@hbut')
  if (contextIndex >= 0) {
    return `/${value.slice(contextIndex).replace(/^\/+/, '')}`
  }

  if (value.startsWith('/uploads/')) return `${CONTEXT_PATH}${value}`
  if (value.startsWith('uploads/')) return `${CONTEXT_PATH}/${value}`
  if (value.startsWith('/')) return value
  return `${CONTEXT_PATH}/uploads/${value}`
}

export const getFileUrl = getImageUrl

export const isImage = (url) => {
  if (!url) return false
  const imageExtensions = ['.jpg', '.jpeg', '.png', '.gif', '.bmp', '.webp', '.svg']
  const lowerUrl = String(url).toLowerCase().split(/[?#]/)[0]
  return imageExtensions.some(extension => lowerUrl.endsWith(extension))
}

export const getFileName = (url) => {
  if (!url) return ''
  try {
    return decodeURIComponent(String(url).split(/[?#]/)[0].split('/').pop())
  } catch {
    return String(url)
  }
}

/**
 * 图片URL处理工具函数
 * 统一处理图片路径，确保图片能正确显示
 */

// 获取基础URL（从API配置中提取）
const getBaseURL = () => {
  // 从 request.js 中的 baseURL 提取基础路径
  // baseURL: 'http://localhost:1010/SCSE@hbut/msi'
  // 图片访问路径应该是: 'http://localhost:1010/SCSE@hbut'
  return 'http://localhost:1010/SCSE@hbut'
}

/**
 * 获取完整的图片URL
 * @param {string} url - 图片路径（可能是相对路径或文件名）
 * @returns {string} 完整的图片URL
 */
export const getImageUrl = (url) => {
  if (!url) return ''
  
  // 如果已经是完整URL（包含http://或https://），直接返回
  if (url.startsWith('http://') || url.startsWith('https://')) {
    return url
  }
  
  const baseURL = getBaseURL() // 'http://localhost:1010/SCSE@hbut'
  const serverBase = 'http://localhost:1010' // 服务器基础URL
  
  // 如果URL已经以/SCSE@hbut开头（后端返回的格式），直接拼接服务器基础URL
  if (url.startsWith('/SCSE@hbut')) {
    return serverBase + url
  }
  
  // 如果URL包含SCSE@hbut但不是以/开头，说明可能已经包含了部分路径
  if (url.includes('SCSE@hbut')) {
    // 提取SCSE@hbut之后的部分
    const match = url.match(/SCSE@hbut[/]?(.+)/)
    if (match) {
      const path = match[1]
      // 确保路径以/开头
      return baseURL + (path.startsWith('/') ? path : '/' + path)
    }
  }
  
  // 如果URL以/开头，直接拼接（如 /uploads/xxx.jpg）
  if (url.startsWith('/')) {
    return baseURL + url
  }
  
  // 如果URL包含 uploads，说明已经是相对路径，直接拼接
  if (url.includes('uploads')) {
    // 如果已经以/开头，直接拼接；否则添加/
    return baseURL + (url.startsWith('/') ? url : '/' + url)
  }
  
  // 否则，假设是文件名，拼接 /uploads/ 前缀
  return baseURL + '/uploads/' + url
}

/**
 * 获取文件完整URL（用于附件等）
 * @param {string} url - 文件路径
 * @returns {string} 完整的文件URL
 */
export const getFileUrl = (url) => {
  return getImageUrl(url)
}

/**
 * 判断是否为图片文件
 * @param {string} url - 文件URL
 * @returns {boolean} 是否为图片
 */
export const isImage = (url) => {
  if (!url) return false
  const imageExtensions = ['.jpg', '.jpeg', '.png', '.gif', '.bmp', '.webp', '.svg']
  const lowerUrl = url.toLowerCase()
  return imageExtensions.some(ext => lowerUrl.includes(ext))
}

/**
 * 获取文件名
 * @param {string} url - 文件URL
 * @returns {string} 文件名
 */
export const getFileName = (url) => {
  if (!url) return ''
  try {
    return decodeURIComponent(url.split('/').pop())
  } catch {
    return url
  }
}


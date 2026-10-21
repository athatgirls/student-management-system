// Never forward a bearer token to a URL outside the application's upload path.
export const privateUploadUrl = (value, origin, baseURL) => {
  try {
    const contextPath = new URL(baseURL, origin).pathname.replace(/\/msi\/?$/, '')
    const url = new URL(value, origin)
    if (url.origin !== origin || url.search || url.hash) return ''
    const prefix = `${contextPath}/uploads/`
    const filename = url.pathname.startsWith(prefix) ? url.pathname.slice(prefix.length) : ''
    if (!/^[a-zA-Z0-9_-]+\.(jpg|jpeg|png|pdf|doc|docx)$/i.test(filename)) return ''
    return url.pathname
  } catch { return '' }
}

export const MAX_ATTACHMENT_BYTES = 10 * 1024 * 1024

export function attachmentError(file) {
  if (!file || !Number.isFinite(file.size) || file.size <= 0) return '请选择非空文件'
  if (file.size > MAX_ATTACHMENT_BYTES) return '单个文件不能超过 10MB'
  if (!/\.(pdf|doc|docx|png|jpe?g)$/i.test(file.name || '')) return '只能上传 PDF、Word 或图片材料'
  return ''
}

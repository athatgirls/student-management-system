import request from './request'

export const listTemplates = management => request.get(management ? '/templates/admin/list' : '/templates/list')
export const uploadTemplate = data => request.post('/templates/admin/upload', data, {
  headers: { 'Content-Type': 'multipart/form-data' }, timeout: 60000
})
export const deleteTemplate = id => request.post(`/templates/admin/${id}/delete`)
export const downloadTemplate = async template => {
  const blob = await request.get(`/templates/${template.id}/download`, { responseType: 'blob' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = template.originalName
  link.click()
  window.setTimeout(() => URL.revokeObjectURL(url), 1000)
}

export function hasAttachments(value) {
  try { const files = JSON.parse(value || '[]'); return Array.isArray(files) && files.length > 0 && files.every(f => typeof f.url === 'string' && f.url.trim()) } catch { return false }
}
export function orderedDates(start, end) {
  if (!start || !end) return true
  const a = new Date(start).getTime(), b = new Date(end).getTime()
  return Number.isFinite(a) && Number.isFinite(b) && b >= a
}
export function taskDateError(fields, values) {
  for (const field of fields || []) {
    if (field.fieldType !== 'date' || !values[field.fieldName]) continue
    if (!Number.isFinite(new Date(values[field.fieldName]).getTime())) return `${field.fieldName}不是有效日期`
    const name = field.fieldName
    const startName = field.notBeforeField || (name.includes('返校') ? name.replace('返校', '离校') : name.includes('结束') ? name.replace('结束', '开始') : '')
    if (startName && !orderedDates(values[startName], values[name])) return `${name}不能早于${startName}`
  }
  return ''
}

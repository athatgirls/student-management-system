/** UTC event timestamps and local deadlines are both rendered in China's 24-hour clock. */
export const formatDateTime = value => {
  if (!value) return ''
  const date = value instanceof Date ? value : new Date(value)
  if (Number.isNaN(+date)) return ''
  // A timezone-less server value is an explicitly entered local deadline/activity time.
  if (typeof value === 'string' && /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}/.test(value) && !/(Z|[+-]\d{2}:\d{2})$/.test(value)) {
    return value.slice(0, 16).replace('T', ' ')
  }
  const parts = new Intl.DateTimeFormat('en-GB', {
    timeZone: 'Asia/Shanghai', year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit', hourCycle: 'h23'
  }).formatToParts(date)
  const fields = Object.fromEntries(parts.map(part => [part.type, part.value]))
  return `${fields.year}-${fields.month}-${fields.day} ${fields.hour}:${fields.minute}`
}

export const formatDate = value => {
  if (!value) return ''
  // Date-only business fields represent a calendar date, not an instant.
  if (typeof value === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(value)) return value
  return formatDateTime(value).slice(0, 10)
}

export const isPastLeaveDate = (value, now = new Date()) => {
  if (!value) return false
  // The date picker supplies local-midnight Date objects; keep its selected day.
  const selected = value instanceof Date
    ? `${value.getFullYear()}-${String(value.getMonth() + 1).padStart(2, '0')}-${String(value.getDate()).padStart(2, '0')}`
    : String(value).slice(0, 10)
  return selected < formatDate(now)
}

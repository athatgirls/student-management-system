/** UTC event timestamps and local deadlines are both rendered in China's 24-hour clock. */
export const formatDateTime = value => {
  if (!value) return ''
  const date = value instanceof Date ? value : new Date(value)
  if (Number.isNaN(+date)) return String(value)
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

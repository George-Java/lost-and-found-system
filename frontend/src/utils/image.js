export function parseUrlList(value) {
  if (!value) return []
  return value
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean)
}

export function joinUrlList(list) {
  if (!Array.isArray(list)) return ''
  return list.map((item) => item.trim()).filter(Boolean).join(',')
}

export function createMessageSocketUrl(token) {
  const configured = import.meta.env.VITE_WS_URL
  const query = `token=${encodeURIComponent(token)}`
  if (configured) {
    const separator = configured.includes('?') ? '&' : '?'
    return `${configured}${separator}${query}`
  }

  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  return `${protocol}//${window.location.host}/ws/messages?${query}`
}

const explicitBackendUrl = import.meta.env.VITE_BACKEND_URL?.trim()
const normalizedBackendUrl = explicitBackendUrl
  ? explicitBackendUrl.replace(/\/+$/, '')
  : ''

function buildBackendUrl(path) {
  if (normalizedBackendUrl) {
    return `${normalizedBackendUrl}${path}`
  }

  return path
}

export const spotifyAuthPath = buildBackendUrl('/oauth2/authorization/spotify')
export const roastApiPath = buildBackendUrl('/api/roast')

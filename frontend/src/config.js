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
export const roastApiPath = buildBackendUrl('/api/roast?limit=10&timeRange=medium_term')
export const userDataApiPath = buildBackendUrl('/api/userdata?limit=10&timeRange=medium_term')

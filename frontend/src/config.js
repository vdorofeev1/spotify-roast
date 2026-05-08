const runtimeBackendUrl = window.__SPOTIFY_ROAST_CONFIG__?.BACKEND_URL
const explicitBackendUrl = runtimeBackendUrl || import.meta.env.VITE_BACKEND_URL || ''

function buildBackendUrl(path) {
  return explicitBackendUrl ? `${explicitBackendUrl}${path}` : path
}

export const spotifyAuthPath = buildBackendUrl('/oauth2/authorization/spotify')
export const roastApiPath = buildBackendUrl('/api/roast?limit=10&timeRange=medium_term')
export const userDataApiPath = buildBackendUrl('/api/userdata?limit=10&timeRange=medium_term')

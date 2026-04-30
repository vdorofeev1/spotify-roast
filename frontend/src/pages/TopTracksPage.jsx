import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import TerminalFrame from '../components/TerminalFrame'
import { spotifyAuthPath, topTracksApiPath } from '../config'

function parseErrorMessage(payload) {
  if (payload && typeof payload === 'object' && typeof payload.message === 'string') {
    return payload.message
  }

  if (payload && typeof payload === 'object' && typeof payload.error === 'string') {
    return payload.error
  }

  return 'Spotify did not return usable top tracks.'
}

function formatDuration(durationMs) {
  const totalSeconds = Math.round(durationMs / 1000)
  const minutes = Math.floor(totalSeconds / 60)
  const seconds = String(totalSeconds % 60).padStart(2, '0')

  return `${minutes}:${seconds}`
}

function getAlbumImage(track) {
  return track.album?.images?.[0]?.url || ''
}

export default function TopTracksPage() {
  const [state, setState] = useState({
    status: 'loading',
    tracks: [],
    message: '',
  })

  useEffect(() => {
    const controller = new AbortController()

    async function loadTopTracks() {
      try {
        const response = await fetch(topTracksApiPath, {
          credentials: 'include',
          signal: controller.signal,
        })

        const payload = await response.json().catch(() => null)

        if (response.ok && Array.isArray(payload?.items)) {
          setState({
            status: payload.items.length > 0 ? 'ready' : 'empty',
            tracks: payload.items,
            message: payload.items.length > 0 ? '' : 'Spotify returned no top tracks for this account.',
          })
          return
        }

        if (response.status === 401) {
          setState({
            status: 'auth',
            tracks: [],
            message: 'Your session is missing or expired. Authenticate with Spotify again.',
          })
          return
        }

        setState({
          status: 'error',
          tracks: [],
          message: parseErrorMessage(payload),
        })
      } catch (error) {
        if (error.name === 'AbortError') {
          return
        }

        setState({
          status: 'error',
          tracks: [],
          message: 'Unable to reach the backend. Start the API and try again.',
        })
      }
    }

    loadTopTracks()

    return () => controller.abort()
  }, [])

  const footer = (
    <div className="footer-row">
      <span>Endpoint: {topTracksApiPath}</span>
      <span>Session: cookies included</span>
    </div>
  )

  if (state.status === 'ready') {
    return (
      <TerminalFrame title="spotify-tracks@results" eyebrow="Top Tracks" footer={footer}>
        <div className="hero-copy hero-copy-compact">
          <h1>Your Spotify top tracks</h1>
          <p>Showing the tracks Spotify ranks highest for your account.</p>
        </div>

        <div className="action-row" style={{ marginBottom: '2rem' }}>
          <Link className="terminal-button" to="/roast">
            <span className="button-prompt" aria-hidden="true">$</span>
            Get Roasted
          </Link>
        </div>

        <ol className="track-list" aria-label="Spotify top tracks">
          {state.tracks.map((track, index) => {
            const imageUrl = getAlbumImage(track)
            const artists = track.artists.map((artist) => artist.name).join(', ')

            return (
              <li className="track-item" key={track.id}>
                <span className="track-rank">{String(index + 1).padStart(2, '0')}</span>
                {imageUrl ? (
                  <img className="album-art" src={imageUrl} alt="" loading="lazy" />
                ) : (
                  <div className="album-art album-art-empty" aria-hidden="true" />
                )}
                <div className="track-main">
                  <h2>{track.name}</h2>
                  <p>{artists}</p>
                </div>
                <div className="track-meta">
                  <span>{track.album.name}</span>
                  <span>{formatDuration(track.durationMs)}</span>
                </div>
              </li>
            )
          })}
        </ol>
      </TerminalFrame>
    )
  }

  return (
    <TerminalFrame title="spotify-tracks@results" eyebrow="Top Tracks" footer={footer}>
      <div className="hero-copy">
        <h1>
          {state.status === 'loading' && 'Pulling your Spotify top tracks.'}
          {state.status === 'empty' && 'Spotify connected. No top tracks were returned.'}
          {state.status === 'auth' && 'Authentication required.'}
          {state.status === 'error' && 'The track request hit an error.'}
        </h1>
        <p>{state.message || 'Asking Spotify for your top tracks...'}</p>
      </div>

      {state.status === 'loading' ? (
        <div className="boot-log" aria-live="polite">
          <p className="boot-line">$ fetch /api/me/top-tracks</p>
          <p className="boot-line boot-line-dim">&gt; waiting for response...</p>
        </div>
      ) : (
        <div className="action-row">
          <a className="terminal-button" href={spotifyAuthPath}>
            <span className="button-prompt" aria-hidden="true">
              $
            </span>
            Reconnect Spotify
          </a>
          <Link className="secondary-link" to="/">
            Return to login
          </Link>
        </div>
      )}
    </TerminalFrame>
  )
}

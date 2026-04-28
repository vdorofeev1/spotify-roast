import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import TerminalFrame from '../components/TerminalFrame'
import { roastApiPath, spotifyAuthPath } from '../config'

function parseErrorMessage(payload) {
  if (payload && typeof payload === 'object' && typeof payload.message === 'string') {
    return payload.message
  }

  return 'The roast pipeline did not return a usable response.'
}

export default function RoastPage() {
  const [state, setState] = useState({
    status: 'loading',
    roastText: '',
    message: '',
  })

  useEffect(() => {
    const controller = new AbortController()

    async function loadRoast() {
      try {
        const response = await fetch(roastApiPath, {
          credentials: 'include',
          signal: controller.signal,
        })

        const payload = await response.json().catch(() => null)

        if (response.ok && payload?.roastText) {
          setState({
            status: 'ready',
            roastText: payload.roastText,
            message: '',
          })
          return
        }

        if (response.status === 401) {
          setState({
            status: 'auth',
            roastText: '',
            message: 'Your session is missing or expired. Authenticate with Spotify again.',
          })
          return
        }

        if (response.status === 404) {
          setState({
            status: 'empty',
            roastText: '',
            message: parseErrorMessage(payload),
          })
          return
        }

        setState({
          status: 'error',
          roastText: '',
          message: parseErrorMessage(payload),
        })
      } catch (error) {
        if (error.name === 'AbortError') {
          return
        }

        setState({
          status: 'error',
          roastText: '',
          message: 'Unable to reach the backend. Start the API and try again.',
        })
      }
    }

    loadRoast()

    return () => controller.abort()
  }, [])

  const footer = (
    <div className="footer-row">
      <span>Endpoint: {roastApiPath}</span>
      <span>Session: cookies included</span>
    </div>
  )

  if (state.status === 'ready') {
    return (
      <TerminalFrame title="spotify-roast@results" eyebrow="Roast Output" footer={footer}>
        <div className="roast-panel">
          <p className="status-pill">analysis complete</p>
          <pre className="roast-copy">{state.roastText}</pre>
        </div>
      </TerminalFrame>
    )
  }

  return (
    <TerminalFrame title="spotify-roast@results" eyebrow="Roast Output" footer={footer}>
      <div className="hero-copy">
        <h1>
          {state.status === 'loading' && 'Pulling your latest roast from the backend.'}
          {state.status === 'empty' && 'Spotify connected. No roast has been generated yet.'}
          {state.status === 'auth' && 'Authentication required.'}
          {state.status === 'error' && 'The roast terminal hit an error.'}
        </h1>
        <p>{state.message || 'Analyzing your top artists and tracks...'}</p>
      </div>

      {state.status === 'loading' ? (
        <div className="boot-log" aria-live="polite">
          <p className="boot-line">$ fetch /api/roast</p>
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

import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import TerminalFrame from '../components/TerminalFrame'
import { spotifyAuthPath, roastApiPath } from '../config'

function parseErrorMessage(payload) {
  if (payload && typeof payload === 'object' && typeof payload.message === 'string') {
    return payload.message
  }

  if (payload && typeof payload === 'object' && typeof payload.error === 'string') {
    return payload.error
  }

  return 'The roasting engine failed to ignite.'
}

export default function RoastPage() {
  const [state, setState] = useState({
    status: 'loading',
    roast: '',
    message: '',
  })

  useEffect(() => {
    const controller = new AbortController()

    async function generateRoast() {
      try {
        const response = await fetch(roastApiPath, {
          credentials: 'include',
          signal: controller.signal,
        })

        const payload = await response.json().catch(() => null)

        if (response.ok && payload?.roast) {
          setState({
            status: 'ready',
            roast: payload.roast,
            message: '',
          })
          return
        }

        if (response.status === 401) {
          setState({
            status: 'auth',
            roast: '',
            message: 'Your session is missing or expired. Authenticate with Spotify again.',
          })
          return
        }

        setState({
          status: 'error',
          roast: '',
          message: parseErrorMessage(payload),
        })
      } catch (error) {
        if (error.name === 'AbortError') {
          return
        }

        setState({
          status: 'error',
          roast: '',
          message: 'Unable to reach the backend. Start the API and try again.',
        })
      }
    }

    generateRoast()

    return () => controller.abort()
  }, [])

  const footer = (
    <div className="footer-row">
      <span>Endpoint: {roastApiPath}</span>
      <span>Engine: gemini-2.5-flash-lite</span>
    </div>
  )

  if (state.status === 'ready') {
    return (
      <TerminalFrame title="spotify-roast@results" eyebrow="The Roast" footer={footer}>
        <div className="hero-copy hero-copy-compact">
          <h1>Your Musical Roast</h1>
          <p>The AI has analyzed your questionable choices.</p>
        </div>

        <div className="roast-content">
          {state.roast.split('\n').map((paragraph, index) => (
            <p key={index} className="roast-paragraph">
              {paragraph}
            </p>
          ))}
        </div>

        <div className="action-row" style={{ marginTop: '2rem' }}>
          <Link className="terminal-button" to="/top-tracks">
            <span className="button-prompt" aria-hidden="true">$</span>
            View Top Tracks
          </Link>
          <Link className="secondary-link" to="/">
            Try again
          </Link>
        </div>
      </TerminalFrame>
    )
  }

  return (
    <TerminalFrame title="spotify-roast@results" eyebrow="The Roast" footer={footer}>
      <div className="hero-copy">
        <h1>
          {state.status === 'loading' && 'Analyzing your questionable taste...'}
          {state.status === 'auth' && 'Authentication required.'}
          {state.status === 'error' && 'The roasting engine hit an error.'}
        </h1>
        <p>{state.message || 'Consulting the AI for maximum damage...'}</p>
      </div>

      {state.status === 'loading' ? (
        <div className="boot-log" aria-live="polite">
          <p className="boot-line">$ fetch /api/roast</p>
          <p className="boot-line boot-line-dim">&gt; waiting for AI response...</p>
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

import TerminalFrame from '../components/TerminalFrame'
import { spotifyAuthPath } from '../config'

const bootLines = [
  '$ boot spotify-roast',
  '> loading sarcasm engine',
  '> calibrating taste profiler',
  '> waiting for Spotify authorization',
]

export default function LoginPage() {
  return (
    <TerminalFrame
      title="spotify-roast@login"
      eyebrow="Terminal Access"
      footer={
        <div className="footer-row">
          <span>OAuth target: Spotify</span>
          <span>Return route: /roast</span>
        </div>
      }
    >
      <div className="hero-copy">
        <h1>Sign in and let the terminal judge your music taste.</h1>
        <p>
          Connect your Spotify account to generate a roast based on your listening history.
          After authentication, you will land directly on the roast screen.
        </p>
      </div>

      <div className="boot-log" aria-label="Startup log">
        {bootLines.map((line) => (
          <p key={line} className="boot-line">
            {line}
          </p>
        ))}
      </div>

      <div className="action-row">
        <a className="terminal-button" href={spotifyAuthPath}>
          <span className="button-prompt" aria-hidden="true">
            $
          </span>
          Login with Spotify
        </a>
        <p className="helper-text">
          Uses the backend OAuth flow at <code>/oauth2/authorization/spotify</code>.
        </p>
      </div>
    </TerminalFrame>
  )
}

import TerminalFrame from '../components/TerminalFrame'
import { spotifyAuthPath } from '../config'

const bootLines = [
  '$ boot spotify-top-tracks',
  '> preparing Spotify session',
  '> requesting top tracks scope',
  '> waiting for Spotify authorization',
]

export default function LoginPage() {
  return (
    <TerminalFrame
      title="spotify-tracks@login"
      eyebrow="Terminal Access"
      footer={
        <div className="footer-row">
          <span>OAuth target: Spotify</span>
          <span>Return route: /roast</span>
        </div>
      }
    >
      <div className="hero-copy">
        <h1>Roast your Spotify taste.</h1>
        <p>
          Connect your Spotify account to get a brutal, AI-powered roast of your listening habits.
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

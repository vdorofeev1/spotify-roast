export default function TerminalFrame({ title, eyebrow, children, footer }) {
  return (
    <main className="screen-shell">
      <section className="terminal-window" aria-label={title}>
        <header className="terminal-header">
          <div className="terminal-lights" aria-hidden="true">
            <span className="terminal-light terminal-light-red" />
            <span className="terminal-light terminal-light-amber" />
            <span className="terminal-light terminal-light-green" />
          </div>
          <p className="terminal-title">{title}</p>
        </header>

        <div className="terminal-body">
          <p className="terminal-eyebrow">{eyebrow}</p>
          {children}
        </div>

        {footer ? <footer className="terminal-footer">{footer}</footer> : null}
      </section>
    </main>
  )
}

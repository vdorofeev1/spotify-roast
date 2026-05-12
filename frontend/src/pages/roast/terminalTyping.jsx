import { useEffect, useMemo, useRef, useState } from 'react'

const TYPE_SPEED_MS = 18
const LINE_PAUSE_MS = 180

export function TypedTerminalLine({ text, className = '', onDone }) {
  const [displayedText, setDisplayedText] = useState('')
  const [isComplete, setIsComplete] = useState(false)
  const onDoneRef = useRef(onDone)

  useEffect(() => {
    onDoneRef.current = onDone
  }, [onDone])

  useEffect(() => {
    let characterIndex = 0
    let timeoutId

    setDisplayedText('')
    setIsComplete(false)

    if (!text) {
      setIsComplete(true)
      onDoneRef.current?.()
      return undefined
    }

    const intervalId = window.setInterval(() => {
      characterIndex += 1
      setDisplayedText(text.slice(0, characterIndex))

      if (characterIndex >= text.length) {
        window.clearInterval(intervalId)
        setIsComplete(true)
        timeoutId = window.setTimeout(() => onDoneRef.current?.(), LINE_PAUSE_MS)
      }
    }, TYPE_SPEED_MS)

    return () => {
      window.clearInterval(intervalId)
      window.clearTimeout(timeoutId)
    }
  }, [text])

  return (
    <p className={`boot-line ${isComplete ? 'boot-line-complete' : ''} ${className}`.trim()}>
      {displayedText}
    </p>
  )
}

export function TypedTranscript({ lines }) {
  const [visibleLineCount, setVisibleLineCount] = useState(lines.length ? 1 : 0)
  const signature = useMemo(
    () => lines.map((line) => `${line.text}:${line.className || ''}`).join('\n'),
    [lines],
  )

  useEffect(() => {
    setVisibleLineCount(lines.length ? 1 : 0)
  }, [lines.length, signature])

  function showNextLine(index) {
    setVisibleLineCount((count) => {
      if (index !== count - 1) {
        return count
      }

      return Math.min(count + 1, lines.length)
    })
  }

  return lines.slice(0, visibleLineCount).map((line, index) => (
    <TypedTerminalLine
      key={`${line.text}-${index}`}
      text={line.text}
      className={line.className}
      onDone={() => showNextLine(index)}
    />
  ))
}

export function StaticTerminalLine({ text, className = '' }) {
  return (
    <p className={`boot-line boot-line-complete ${className}`.trim()}>
      {text}
    </p>
  )
}

import { useCallback, useEffect, useRef } from 'react'

export function useAutoScroll({ isEnabled, scrollKey }) {
  const scrollerRef = useRef(null)
  const contentRef = useRef(null)
  const scrollFrameRef = useRef(null)

  const scrollToBottom = useCallback(() => {
    window.cancelAnimationFrame(scrollFrameRef.current)
    scrollFrameRef.current = window.requestAnimationFrame(() => {
      const scroller = scrollerRef.current

      if (!scroller) {
        return
      }

      scroller.scrollTo({
        top: scroller.scrollHeight,
        behavior: 'smooth',
      })
    })
  }, [])

  useEffect(() => {
    return () => {
      window.cancelAnimationFrame(scrollFrameRef.current)
    }
  }, [])

  useEffect(() => {
    if (isEnabled) {
      scrollToBottom()
    }
  }, [isEnabled, scrollKey, scrollToBottom])

  useEffect(() => {
    const content = contentRef.current

    if (!content || typeof ResizeObserver === 'undefined') {
      return undefined
    }

    const observer = new ResizeObserver(() => {
      scrollToBottom()
    })
    observer.observe(content)

    return () => observer.disconnect()
  }, [scrollKey, scrollToBottom])

  return {
    contentRef,
    scrollerRef,
  }
}

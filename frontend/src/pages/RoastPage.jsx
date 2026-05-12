import { useState } from 'react'
import { Link } from 'react-router-dom'
import TerminalFrame from '../components/TerminalFrame'
import { spotifyAuthPath } from '../config'
import { InteractiveRoastLog, LoadingRoastLog } from './roast/RoastLog'
import { buildQuestionScript, buildUserDataLines, getRoastHeading } from './roast/roastContent'
import { useAutoScroll } from './roast/useAutoScroll'
import { useRoastRequest, useUserDataRequest } from './roast/useRoastRequests'

export default function RoastPage() {
  const roastState = useRoastRequest()
  const userDataState = useUserDataRequest()
  const [answers, setAnswers] = useState([])
  const [completedReactionCount, setCompletedReactionCount] = useState(0)
  const [brainDamageIndex] = useState(() => Math.floor(Math.random() * 13) + 83)

  const loadingLines = buildUserDataLines(userDataState.data, userDataState.status)
  const questions = buildQuestionScript(userDataState.status === 'ready' ? userDataState.data : null)
  const visibleAnswers = answers.slice(0, Math.min(answers.length, completedReactionCount + 1))
  const canShowNextPrompt = answers.length === completedReactionCount
  const activeQuestion = canShowNextPrompt ? questions[answers.length] : null
  const hasAnsweredAllQuestions = userDataState.status !== 'loading' && answers.length >= questions.length
  const heading = getRoastHeading({
    roastStatus: roastState.status,
    userDataStatus: userDataState.status,
    hasAnsweredAllQuestions,
  })
  const shouldAutoScroll = (
    userDataState.status === 'loading'
    || roastState.status === 'loading'
    || roastState.status === 'ready'
  )
  const scrollKey = [
    activeQuestion?.id || '',
    answers.length,
    completedReactionCount,
    roastState.status,
    userDataState.status,
  ].join(':')
  const { contentRef, scrollerRef } = useAutoScroll({
    isEnabled: shouldAutoScroll,
    scrollKey,
  })

  function selectAnswer(option) {
    if (!activeQuestion) {
      return
    }

    setAnswers((currentAnswers) => [
      ...currentAnswers,
      {
        question: activeQuestion.prompt,
        option,
      },
    ])
  }

  function completeReaction(index) {
    setCompletedReactionCount((count) => Math.max(count, index + 1))
  }

  return (
    <TerminalFrame title="spotify-roast@results" eyebrow="The Roast">
      <div className="hero-copy">
        <h1>{heading}</h1>
        <p>{roastState.message || 'Answer the prompt. The terminal will not continue until you do.'}</p>
      </div>

      {userDataState.status === 'loading' ? (
        <LoadingRoastLog lines={loadingLines} scrollerRef={scrollerRef} contentRef={contentRef} />
      ) : roastState.status === 'loading' || roastState.status === 'ready' ? (
        <InteractiveRoastLog
          activeQuestion={activeQuestion}
          answers={answers}
          brainDamageIndex={brainDamageIndex}
          canShowNextPrompt={canShowNextPrompt}
          contentRef={contentRef}
          hasAnsweredAllQuestions={hasAnsweredAllQuestions}
          onReactionDone={completeReaction}
          onSelectAnswer={selectAnswer}
          roastState={roastState}
          scrollerRef={scrollerRef}
          userData={userDataState.data}
          visibleAnswers={visibleAnswers}
        />
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

      {hasAnsweredAllQuestions && roastState.status === 'ready' ? (
        <div className="action-row">
          <Link className="secondary-link" to="/">
            Try again
          </Link>
        </div>
      ) : null}
    </TerminalFrame>
  )
}

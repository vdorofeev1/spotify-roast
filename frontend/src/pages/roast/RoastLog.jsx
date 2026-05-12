import { useEffect, useState } from 'react'
import { buildFinalTranscript, openingLines } from './roastContent'
import { StaticTerminalLine, TypedTerminalLine, TypedTranscript } from './terminalTyping'

function ChatScrollAnchor() {
  return <div className="chat-scroll-anchor" aria-hidden="true" />
}

function AnswerHistoryEntry({ answer, index, onReactionDone }) {
  return (
    <div className="question-history">
      <StaticTerminalLine
        text={`> q${index + 1}: ${answer.question}`}
        className="terminal-question-line"
      />
      <StaticTerminalLine text={`< ${answer.option.label}`} className="user-answer-line" />
      <TypedTerminalLine
        text={`> ${answer.option.reaction}`}
        className="terminal-reaction-line"
        onDone={onReactionDone}
      />
    </div>
  )
}

function ActiveQuestionBlock({ question, questionNumber, onSelectAnswer }) {
  const [showOptions, setShowOptions] = useState(false)

  useEffect(() => {
    setShowOptions(false)
  }, [question.id])

  return (
    <div className="active-question">
      <TypedTerminalLine
        text={`> q${questionNumber}: ${question.prompt}`}
        className="terminal-question-line"
        onDone={() => setShowOptions(true)}
      />
      {showOptions ? (
        <div className="answer-options">
          {question.options.map((option, index) => (
            <button
              className="answer-option"
              key={option.label}
              type="button"
              onClick={() => onSelectAnswer(option)}
            >
              <span aria-hidden="true">{index + 1}.</span>
              {option.label}
            </button>
          ))}
        </div>
      ) : null}
    </div>
  )
}

export function LoadingRoastLog({ lines, scrollerRef, contentRef }) {
  return (
    <div className="boot-log roast-log chat-scroll-container" aria-live="polite" ref={scrollerRef}>
      <div className="chat-log-content" ref={contentRef}>
        <TypedTranscript lines={lines.map((line) => ({ text: line }))} />
        <ChatScrollAnchor />
      </div>
    </div>
  )
}

export function InteractiveRoastLog({
  activeQuestion,
  answers,
  brainDamageIndex,
  canShowNextPrompt,
  contentRef,
  hasAnsweredAllQuestions,
  onReactionDone,
  onSelectAnswer,
  roastState,
  scrollerRef,
  userData,
  visibleAnswers,
}) {
  return (
    <div
      className="boot-log roast-log interactive-roast-log chat-scroll-container"
      aria-live="polite"
      ref={scrollerRef}
    >
      <div className="chat-log-content" ref={contentRef}>
        {openingLines.map((line, index) => (
          <StaticTerminalLine
            key={`${line}-${index}`}
            text={line}
            className={line.startsWith('>') ? 'terminal-processing-line' : ''}
          />
        ))}
        <TypedTerminalLine text="$ spotify-roast --interrogate-user" />
        {visibleAnswers.map((answer, index) => (
          <AnswerHistoryEntry
            answer={answer}
            index={index}
            key={`${answer.question}-${index}`}
            onReactionDone={() => onReactionDone(index)}
          />
        ))}

        {activeQuestion ? (
          <ActiveQuestionBlock
            question={activeQuestion}
            questionNumber={answers.length + 1}
            onSelectAnswer={onSelectAnswer}
          />
        ) : hasAnsweredAllQuestions && canShowNextPrompt ? (
          <TypedTranscript
            lines={buildFinalTranscript({
              roast: roastState.roast,
              roastStatus: roastState.status,
              userData,
              brainDamageIndex,
            })}
          />
        ) : null}
        <ChatScrollAnchor />
      </div>
    </div>
  )
}

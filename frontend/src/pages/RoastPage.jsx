import { useCallback, useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import TerminalFrame from '../components/TerminalFrame'
import { spotifyAuthPath, roastApiPath, userDataApiPath } from '../config'

const openingLines = [
  '$ spotify auth status',
  '> you successfully logged! now lets examine your listening data...',
]

const pendingDataLines = [
  '> indexing tracks Spotify swears are your favorites...',
  '> checking whether this is a playlist or a cry for help...',
  '> waiting for the listening profile...',
]

const fallbackQuestions = [
  {
    id: 'username',
    prompt: 'Is your Spotify username supposed to be funny?',
    options: [
      {
        label: 'no, its not funny',
        reaction: "yes, that's what I thought.",
      },
      {
        label: 'no, its my younger brother account',
        reaction: 'are u kidding? ok nvm...',
      },
    ],
  },
  {
    id: 'taste',
    prompt: 'How should I interpret this listening history?',
    options: [
      {
        label: 'as a cry for help',
        reaction: 'finally, some honesty in this terminal.',
      },
      {
        label: 'as character development',
        reaction: 'lol... sure, call it growth.',
      },
    ],
  },
]

const TYPE_SPEED_MS = 18
const LINE_PAUSE_MS = 180

function parseErrorMessage(payload) {
  if (payload && typeof payload === 'object' && typeof payload.message === 'string') {
    return payload.message
  }

  if (payload && typeof payload === 'object' && typeof payload.error === 'string') {
    return payload.error
  }

  return 'The roasting engine failed to ignite.'
}

function getArtistNames(track) {
  return track?.artists?.map((artist) => artist.name).filter(Boolean) || []
}

function getTopArtistName(userData) {
  const artistCounts = new Map()

  for (const artist of userData?.topArtists || []) {
    if (artist.name) {
      artistCounts.set(artist.name, (artistCounts.get(artist.name) || 0) + 3)
    }
  }

  for (const track of userData?.topTracks || []) {
    for (const name of getArtistNames(track)) {
      artistCounts.set(name, (artistCounts.get(name) || 0) + 1)
    }
  }

  return [...artistCounts.entries()].sort((a, b) => b[1] - a[1])[0]?.[0] || 'sad songs'
}

function buildUserDataLines(userData, status) {
  if (status === 'loading') {
    return [...openingLines, ...pendingDataLines]
  }

  if (status === 'error') {
    return [
      ...openingLines,
      '> /api/userdata did not cooperate, so I am judging the loading spinner instead...',
      '> the AI is still writing your final roast...',
    ]
  }

  const profileName = userData?.profile?.displayName || userData?.profile?.id || 'your username'
  const topArtistName = getTopArtistName(userData)
  const recentTrack = userData?.recentlyPlayed?.[0]?.track
  const genres = [...new Set((userData?.topArtists || []).flatMap((artist) => artist.genres || []))]
  const artistLine = topArtistName.toLowerCase() === 'drake'
    ? 'I see a lot of Drake.. are u ok?'
    : `I see a lot of ${topArtistName}.. are u ok?`

  return [
    ...openingLines,
    `> ${artistLine}`,
    `> Do you think "${profileName}" is really funny? Ok nvm...`,
    recentTrack ? `> recently played: "${recentTrack.name}". Evidence is fresh.` : null,
    genres.length ? `> genre tags found: ${genres.slice(0, 3).join(', ')}.` : null,
    '> roast generation is still running. This may take a moment...',
  ].filter(Boolean)
}

function buildQuestionScript(userData) {
  if (!userData) {
    return fallbackQuestions
  }

  const profileName = userData?.profile?.displayName || userData?.profile?.id || 'your username'
  const topArtistName = getTopArtistName(userData)
  const recentTrack = userData?.recentlyPlayed?.[0]?.track
  const genres = [...new Set((userData?.topArtists || []).flatMap((artist) => artist.genres || []))]

  return [
    {
      id: 'username',
      prompt: `Do you think "${profileName}" is really funny?`,
      options: [
        {
          label: 'no, its not funny',
          reaction: "yes, that's what I thought.",
        },
        {
          label: 'no, its my younger brother account',
          reaction: 'are u kidding? ok nvm...',
        },
      ],
    },
    {
      id: 'top_artist',
      prompt: `I see a lot of ${topArtistName}. Are u ok?`,
      options: [
        {
          label: 'no, but the algorithm understands me',
          reaction: 'the algorithm needs a wellness check too.',
        },
        {
          label: 'yes, this is just my main character phase',
          reaction: 'main character? this is a loading screen at best.',
        },
      ],
    },
    topTrack
      ? {
          id: 'top_track',
          prompt: `Your top track is "${topTrack.name}" by ${formatArtists(topTrack)}. Explain yourself.`,
          options: [
            {
              label: 'it started as a joke and became a lifestyle',
              reaction: 'dangerous sentence. historically terrible outcome.',
            },
            {
              label: 'i was emotionally unavailable and had wifi',
              reaction: 'that explains the stream count and the personality.',
            },
          ],
        }
      : null,
    recentTrack
      ? {
          id: 'recent_track',
          prompt: `You recently played "${recentTrack.name}". Was that intentional?`,
          options: [
            {
              label: 'yes, unfortunately',
              reaction: 'unfortunately is carrying this whole interview.',
            },
            {
              label: 'no, spotify was holding me hostage',
              reaction: 'blink twice if the playlist is still in the room.',
            },
          ],
        }
      : null,
    genres.length
      ? {
          id: 'genres',
          prompt: `Your genre tags say ${genres.slice(0, 3).join(', ')}. What happened here?`,
          options: [
            {
              label: 'i contain multitudes and poor decisions',
              reaction: 'mostly the second thing, but continue.',
            },
            {
              label: 'i let playlists raise me',
              reaction: 'that explains the lack of supervision.',
            },
          ],
        }
      : null,
  ].filter(Boolean)
}

function extractTasteProfile(roast) {
  return roast
    .split('\n')
    .map((line) => line.trim())
    .find((line) => line.toLowerCase().startsWith('taste profile:'))
    ?.replace(/^taste profile:\s*/i, '')
    .trim()
}

function buildTasteProfile(userData, roast, brainDamageIndex) {
  const profileName = userData?.profile?.displayName || userData?.profile?.id || 'unknown user'
  const topArtistName = userData?.topArtists?.[0]?.name || getTopArtistName(userData)
  const tasteProfile = extractTasteProfile(roast) || 'unknown-profile'

  return [
    {
      text: '> taste_profile --summary',
      className: 'terminal-muted-line',
    },
    {
      text: '{',
      className: 'taste-profile-line',
    },
    {
      text: `  taste_profile: ${tasteProfile},`,
      className: 'taste-profile-line',
    },
    {
      text: `  user: ${profileName},`,
      className: 'taste-profile-line',
    },
    {
      text: '  profile_picture: unfunny,',
      className: 'taste-profile-line',
    },
    {
      text: `  primary_symptom: ${topArtistName},`,
      className: 'taste-profile-line',
    },
    {
      text: `  brain_damage_index: ${brainDamageIndex}`,
      className: 'taste-profile-line',
    },
    {
      text: '}',
      className: 'taste-profile-line',
    },
  ]
}

function splitRoastLines(roast) {
  return roast
    .split('\n')
    .map((line) => line.trim())
    .filter(Boolean)
    .filter((line) => !line.toLowerCase().startsWith('taste profile:'))
    .map((line) => `> ${line}`)
}

function TypedTerminalLine({ text, className = '', onDone }) {
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

function TypedTranscript({ lines }) {
  const [visibleLineCount, setVisibleLineCount] = useState(lines.length ? 1 : 0)
  const signature = lines.map((line) => `${line.text}:${line.className || ''}`).join('\n')

  useEffect(() => {
    setVisibleLineCount(lines.length ? 1 : 0)
  }, [lines.length, signature])

  function showNextLine(index) {
    if (index === visibleLineCount - 1 && visibleLineCount < lines.length) {
      setVisibleLineCount((count) => Math.min(count + 1, lines.length))
    }
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

function StaticTerminalLine({ text, className = '' }) {
  return (
    <p className={`boot-line boot-line-complete ${className}`.trim()}>
      {text}
    </p>
  )
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

export default function RoastPage() {
  const chatScrollerRef = useRef(null)
  const chatContentRef = useRef(null)
  const chatEndRef = useRef(null)
  const scrollFrameRef = useRef(null)
  const [state, setState] = useState({
    status: 'loading',
    roast: '',
    message: '',
  })
  const [userDataState, setUserDataState] = useState({
    status: 'loading',
    data: null,
  })
  const [answers, setAnswers] = useState([])
  const [completedReactionCount, setCompletedReactionCount] = useState(0)
  const [brainDamageIndex] = useState(() => Math.floor(Math.random() * 13) + 83)

  const scrollChatToBottom = useCallback(() => {
    window.cancelAnimationFrame(scrollFrameRef.current)
    scrollFrameRef.current = window.requestAnimationFrame(() => {
      const chatScroller = chatScrollerRef.current

      if (!chatScroller) {
        return
      }

      chatScroller.scrollTo({
        top: chatScroller.scrollHeight,
        behavior: 'smooth',
      })
    })
  }, [])

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

  useEffect(() => {
    return () => {
      window.cancelAnimationFrame(scrollFrameRef.current)
    }
  }, [])

  useEffect(() => {
    const controller = new AbortController()

    async function loadUserData() {
      try {
        const response = await fetch(userDataApiPath, {
          credentials: 'include',
          signal: controller.signal,
        })
        const payload = await response.json().catch(() => null)

        if (response.ok && payload) {
          setUserDataState({ status: 'ready', data: payload })
          return
        }

        setUserDataState({ status: 'error', data: null })
      } catch (error) {
        if (error.name !== 'AbortError') {
          setUserDataState({ status: 'error', data: null })
        }
      }
    }

    loadUserData()

    return () => controller.abort()
  }, [])

  const loadingLines = buildUserDataLines(userDataState.data, userDataState.status)
  const questions = buildQuestionScript(userDataState.status === 'ready' ? userDataState.data : null)
  const visibleAnswers = answers.slice(0, Math.min(answers.length, completedReactionCount + 1))
  const canShowNextPrompt = answers.length === completedReactionCount
  const activeQuestion = canShowNextPrompt ? questions[answers.length] : null
  const hasAnsweredAllQuestions = userDataState.status !== 'loading' && answers.length >= questions.length
  const heading = (() => {
    if (state.status === 'auth') {
      return 'Authentication required.'
    }

    if (state.status === 'error') {
      return 'The roasting engine hit an error.'
    }

    if (userDataState.status === 'loading') {
      return 'Analyzing your questionable taste...'
    }

    if (!hasAnsweredAllQuestions) {
      return 'Interactive roast interview.'
    }

    if (state.status === 'loading') {
      return 'Compiling terminal damage report...'
    }

    return 'Your musical roast.'
  })()

  useEffect(() => {
    if (userDataState.status === 'loading' || state.status === 'loading' || state.status === 'ready') {
      scrollChatToBottom()
    }
  }, [
    activeQuestion?.id,
    answers.length,
    completedReactionCount,
    scrollChatToBottom,
    state.status,
    userDataState.status,
  ])

  useEffect(() => {
    const chatContent = chatContentRef.current

    if (!chatContent || typeof ResizeObserver === 'undefined') {
      return undefined
    }

    const observer = new ResizeObserver(() => {
      scrollChatToBottom()
    })
    observer.observe(chatContent)

    return () => observer.disconnect()
  }, [scrollChatToBottom, state.status, userDataState.status])

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

  return (
    <TerminalFrame title="spotify-roast@results" eyebrow="The Roast">
      <div className="hero-copy">
        <h1>{heading}</h1>
        <p>{state.message || 'Answer the prompt. The terminal will not continue until you do.'}</p>
      </div>

      {userDataState.status === 'loading' ? (
        <div className="boot-log roast-log chat-scroll-container" aria-live="polite" ref={chatScrollerRef}>
          <div className="chat-log-content" ref={chatContentRef}>
            <TypedTranscript lines={loadingLines.map((line) => ({ text: line }))} />
            <div className="chat-scroll-anchor" ref={chatEndRef} aria-hidden="true" />
          </div>
        </div>
      ) : state.status === 'loading' || state.status === 'ready' ? (
        <div
          className="boot-log roast-log interactive-roast-log chat-scroll-container"
          aria-live="polite"
          ref={chatScrollerRef}
        >
          <div className="chat-log-content" ref={chatContentRef}>
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
                onReactionDone={() => {
                  setCompletedReactionCount((count) => Math.max(count, index + 1))
                }}
              />
            ))}

            {activeQuestion ? (
              <ActiveQuestionBlock
                question={activeQuestion}
                questionNumber={answers.length + 1}
                onSelectAnswer={selectAnswer}
              />
            ) : hasAnsweredAllQuestions && canShowNextPrompt ? (
              <TypedTranscript
                lines={[
                  {
                    text: '> all answers received. judging without mercy...',
                    className: 'terminal-muted-line',
                  },
                  ...(state.status === 'loading'
                    ? [
                        {
                          text: '> roast generation is still running. This may take a moment...',
                          className: 'terminal-processing-line',
                        },
                      ]
                    : [
                        ...splitRoastLines(state.roast).map((line) => ({
                          text: line,
                          className: 'roast-terminal-line',
                        })),
                        ...buildTasteProfile(userDataState.data, state.roast, brainDamageIndex).map((line) => ({
                          text: line.text,
                          className: line.className,
                        })),
                      ]),
                ]}
              />
            ) : null}
            <div className="chat-scroll-anchor" ref={chatEndRef} aria-hidden="true" />
          </div>
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

      {hasAnsweredAllQuestions && state.status === 'ready' ? (
        <div className="action-row">
          <Link className="secondary-link" to="/">
            Try again
          </Link>
        </div>
      ) : null}
    </TerminalFrame>
  )
}

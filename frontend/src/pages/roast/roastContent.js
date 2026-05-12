export const openingLines = [
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

export function parseErrorMessage(payload) {
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

function formatArtists(track) {
  const artistNames = getArtistNames(track)
  return artistNames.length ? artistNames.join(', ') : 'unknown artists'
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

function getProfileName(userData, fallback = 'your username') {
  return userData?.profile?.displayName || userData?.profile?.id || fallback
}

function getGenres(userData) {
  return [...new Set((userData?.topArtists || []).flatMap((artist) => artist.genres || []))]
}

export function buildUserDataLines(userData, status) {
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

  const profileName = getProfileName(userData)
  const topArtistName = getTopArtistName(userData)
  const recentTrack = userData?.recentlyPlayed?.[0]?.track
  const genres = getGenres(userData)
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

export function buildQuestionScript(userData) {
  if (!userData) {
    return fallbackQuestions
  }

  const profileName = getProfileName(userData)
  const topArtistName = getTopArtistName(userData)
  const topTrack = userData?.topTracks?.[0]
  const recentTrack = userData?.recentlyPlayed?.[0]?.track
  const genres = getGenres(userData)

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

export function splitRoastLines(roast) {
  return roast
    .split('\n')
    .map((line) => line.trim())
    .filter(Boolean)
    .filter((line) => !line.toLowerCase().startsWith('taste profile:'))
    .map((line) => `> ${line}`)
}

function extractTasteProfile(roast) {
  return roast
    .split('\n')
    .map((line) => line.trim())
    .find((line) => line.toLowerCase().startsWith('taste profile:'))
    ?.replace(/^taste profile:\s*/i, '')
    .trim()
}

export function buildTasteProfile(userData, roast, brainDamageIndex) {
  const profileName = getProfileName(userData, 'unknown user')
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

export function buildFinalTranscript({ roast, userData, brainDamageIndex, roastStatus }) {
  const waitingLines = roastStatus === 'loading'
    ? [
        {
          text: '> roast generation is still running. This may take a moment...',
          className: 'terminal-processing-line',
        },
      ]
    : [
        ...splitRoastLines(roast).map((line) => ({
          text: line,
          className: 'roast-terminal-line',
        })),
        ...buildTasteProfile(userData, roast, brainDamageIndex),
      ]

  return [
    {
      text: '> all answers received. judging without mercy...',
      className: 'terminal-muted-line',
    },
    ...waitingLines,
  ]
}

export function getRoastHeading({ roastStatus, userDataStatus, hasAnsweredAllQuestions }) {
  if (roastStatus === 'auth') {
    return 'Authentication required.'
  }

  if (roastStatus === 'error') {
    return 'The roasting engine hit an error.'
  }

  if (userDataStatus === 'loading') {
    return 'Analyzing your questionable taste...'
  }

  if (!hasAnsweredAllQuestions) {
    return 'Interactive roast interview.'
  }

  if (roastStatus === 'loading') {
    return 'Compiling terminal damage report...'
  }

  return 'Your musical roast.'
}

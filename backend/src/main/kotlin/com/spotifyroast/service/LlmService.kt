package com.spotifyroast.service

import com.spotifyroast.dto.GeminiContent
import com.spotifyroast.dto.GeminiPart
import com.spotifyroast.dto.GeminiRequest
import com.spotifyroast.dto.GeminiResponse
import com.spotifyroast.dto.UserSpotifyDataResponse
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.body

@Service
class LlmService(
    private val userDataService: UserDataService,
    @Qualifier("geminiRestClient")
    private val geminiRestClient: RestClient,
    @Value("\${gemini.api-key}")
    private val apiKey: String,
    @Value("\${gemini.model}")
    private val model: String
) {

    fun generateRoast(limit: Int, timeRange: String): String {
        val data = userDataService.getSpotifyData(limit, timeRange)
        return generateRoastFromData(data)
    }

    fun generateRoastFromData(data: UserSpotifyDataResponse): String {
        val prompt = createPrompt(data)

        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(
                        GeminiPart(text = prompt)
                    )
                )
            )
        )

        val response = geminiRestClient.post()
            .uri("/v1beta/models/$model:generateContent")
            .header("x-goog-api-key", apiKey)
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .retrieve()
            .body<GeminiResponse>()

        return response?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            ?: "I'm too stunned by your music taste to even come up with a roast. (Error generating roast)"
    }

    private fun createPrompt(data: UserSpotifyDataResponse): String {
        val topTracks = data.topTracks.joinToString("\n") { "- ${it.name} by ${it.artists.joinToString { a -> a.name }}" }
        val topArtists = data.topArtists.joinToString("\n") { "- ${it.name} (Genres: ${it.genres.joinToString()})" }
        val recentlyPlayed = data.recentlyPlayed.joinToString("\n") { "- ${it.track.name} by ${it.track.artists.joinToString { a -> a.name }}" }

        return """
            Roast my Spotify music taste. Be sarcastic, mean, and very specific.
Don't hold back. Use the following data about my listening habits:

My Top Artists:
$topArtists

My Top Tracks:
$topTracks

Recently Played Tracks:
$recentlyPlayed

Give me a single, cohesive, and brutal roast in a few paragraphs.
You should be very specific and patterns, contradictions, guilty pleasures, genre overlaps, and repeated behaviors from the provided data.
You can use generic observations.
Maximum 2 sentences.

After the roast, output exactly one final line in this format:

Taste Profile: word1-word2-word3-word4

Rules for the Taste Profile:

- All words must be directly inspired by actual patterns in the provided Spotify data.
- All words are in lowercase.
- Generate exactly 4 words separated only by hyphens.
- The profile must feel brutally accurate, funny, culturally aware, and slightly insulting.
- Each word must be directly inspired by actual patterns in the provided Spotify data:
  artists, genres, moods, eras, lyrical themes, cultural influences, contradictions, guilty pleasures, and repeated listening behavior.
- Do NOT generate random aesthetic words.
- Do NOT use generic labels like "musiclover", "cool", "sad", "alternative", "vibes", or "eclectic".
- At least 2 of the 4 words must clearly connect to real patterns found in this specific dataset.
- Prefer short, punchy archetype words that sound like internet culture, subcultures, personality types, lifestyle aesthetics, or music-scene stereotypes.
- Prefer words from this archetype vocabulary whenever they genuinely fit the data:

doomscroll, brainrotted, chronicallyonline, algorithmfed, hyperfixated, overstimulated, playlistgoblin, fypcertified,
softnarcissist, avoidant, attachmentissues, overthinker, emotionallyunavailable, validationseeker, maincharacter, peoplepleaser,
matcha, oatmilk, pilates, journaling, totebag, filmcamera, sundayreset, hiking, nighttrain, hostelcore,
thc-inspired, nicotinecoded, redbullfuelled, microdosing, afterhours, sleepdeprived, clubbathroom, rooftopsmoker,
vinylsnob, traptourist, indietourist, fakeunderground, genrehopper, playlistcurator, deepcutflexer, auxcorddictator,
postsoviet, eastbloc, berlintechno, parisjazz, tokyodreampop, brooklynindie, scandinavianwinter, slavicnostalgia,
y2k, thriftstore, softgrunge, coachella, silverjewelry, pinterestboard,
doomer, artschooldropout, poetryat2am, cigarettephilosophy, rainwindow, existentialcommute, foreignfilm.

- You may invent new archetype words ONLY if they are more accurate than the provided vocabulary.
- The final profile must feel like something people would screenshot and send to friends because it is painfully specific.
        """.trimIndent()
    }
}

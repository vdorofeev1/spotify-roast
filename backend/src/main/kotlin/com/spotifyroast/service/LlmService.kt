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
            .uri("/v1beta/models/$model:generateContent?key=$apiKey")
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
        """.trimIndent()
    }
}

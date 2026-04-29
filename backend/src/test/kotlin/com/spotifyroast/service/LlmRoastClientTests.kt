package com.spotifyroast.service

import com.spotifyroast.config.LlmProperties
import com.spotifyroast.dto.RoastData
import com.spotifyroast.dto.SpotifyAlbumSimple
import com.spotifyroast.dto.SpotifyArtist
import com.spotifyroast.dto.SpotifyArtistSimple
import com.spotifyroast.dto.SpotifyAudioFeatures
import com.spotifyroast.dto.SpotifyAudioFeaturesResponse
import com.spotifyroast.dto.SpotifyImage
import com.spotifyroast.dto.SpotifyPlayHistory
import com.spotifyroast.dto.SpotifyRecentlyPlayedResponse
import com.spotifyroast.dto.SpotifyTopArtistsResponse
import com.spotifyroast.dto.SpotifyTopTracksResponse
import com.spotifyroast.dto.SpotifyTrack
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.content
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import tools.jackson.module.kotlin.jacksonObjectMapper

class LlmRoastClientTests {

    private val objectMapper = jacksonObjectMapper()

    @Test
    fun `generates roast through gemini api`() {
        val baseUrl = "https://generativelanguage.googleapis.com/v1beta"
        val builder = RestClient.builder().baseUrl(baseUrl)
        val server = MockRestServiceServer.bindTo(builder).build()
        val client = LlmRoastClient(
            llmRestClient = builder.build(),
            llmProperties = LlmProperties(apiKey = "gemini-key", model = "gemini-2.5-flash-lite", maxOutputTokens = 500),
            objectMapper = objectMapper,
        )

        server.expect(requestTo("$baseUrl/models/gemini-2.5-flash-lite:generateContent"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("x-goog-api-key", "gemini-key"))
            .andExpect(content().string(containsString("\"maxOutputTokens\":500")))
            .andExpect(content().string(containsString("Radiohead")))
            .andExpect(content().string(containsString("systemInstruction")))
            .andRespond(
                withSuccess(
                    """
                    {
                      "candidates": [
                        {
                          "content": {
                            "parts": [
                              {
                                "text": "Your Spotify is a rain cloud with a loyalty card."
                              }
                            ],
                            "role": "model"
                          },
                          "finishReason": "STOP"
                        }
                      ]
                    }
                    """.trimIndent(),
                    MediaType.APPLICATION_JSON,
                ),
            )

        val roast = client.generateRoast(roastData())

        assertEquals("Your Spotify is a rain cloud with a loyalty card.", roast)
        server.verify()
    }

    @Test
    fun `requires api key before calling llm`() {
        val builder = RestClient.builder().baseUrl("https://api.openai.com/v1")
        val server = MockRestServiceServer.bindTo(builder).build()
        val client = LlmRoastClient(
            llmRestClient = builder.build(),
            llmProperties = LlmProperties(apiKey = "", model = "test-model"),
            objectMapper = objectMapper,
        )

        assertThrows(IllegalArgumentException::class.java) {
            client.generateRoast(roastData())
        }
        server.verify()
    }

    private fun roastData(): RoastData {
        val artist = SpotifyArtist(
            id = "artist-1",
            name = "Radiohead",
            genres = listOf("alternative rock", "art rock"),
            popularity = 82,
            images = listOf(SpotifyImage(url = "https://example.com/radiohead.jpg", height = 640, width = 640)),
        )
        val track = SpotifyTrack(
            id = "track-1",
            name = "No Surprises",
            artists = listOf(SpotifyArtistSimple(id = "artist-1", name = "Radiohead")),
            album = SpotifyAlbumSimple(id = "album-1", name = "OK Computer", images = emptyList()),
            popularity = 78,
            durationMs = 229000,
        )

        return RoastData(
            topArtists = SpotifyTopArtistsResponse(items = listOf(artist)),
            topTracks = SpotifyTopTracksResponse(items = listOf(track)),
            audioFeatures = SpotifyAudioFeaturesResponse(
                audioFeatures = listOf(
                    SpotifyAudioFeatures(
                        id = "track-1",
                        danceability = 0.25f,
                        energy = 0.39f,
                        key = 5,
                        loudness = -10.2f,
                        mode = 1,
                        speechiness = 0.03f,
                        acousticness = 0.16f,
                        instrumentalness = 0.01f,
                        liveness = 0.11f,
                        valence = 0.12f,
                        tempo = 76.0f,
                    ),
                ),
            ),
            recentlyPlayed = SpotifyRecentlyPlayedResponse(
                items = listOf(
                    SpotifyPlayHistory(
                        track = track,
                        playedAt = "2026-04-29T10:15:30Z",
                    ),
                ),
            ),
        )
    }
}

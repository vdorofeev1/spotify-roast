package com.spotifyroast.service

import com.spotifyroast.dto.UserSpotifyDataResponse
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.client.ExpectedCount.once
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.*
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient

class LlmServiceTests {

    private val userDataService = mockk<UserDataService>()
    private val builder = RestClient.builder().baseUrl("https://generativelanguage.googleapis.com")
    private val server = MockRestServiceServer.bindTo(builder).build()
    private val model = "gemini-1.5-flash"
    private val apiKey = "test-api-key"
    
    private val llmService = LlmService(
        userDataService = userDataService,
        geminiRestClient = builder.build(),
        apiKey = apiKey,
        model = model
    )

    @Test
    fun `generates roast from data using Gemini API`() {
        val data = UserSpotifyDataResponse(
            profile = null,
            topTracks = emptyList(),
            topArtists = emptyList(),
            recentlyPlayed = emptyList()
        )

        server.expect(once(), requestTo("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"))
            .andExpect(method(org.springframework.http.HttpMethod.POST))
            .andExpect(header("Content-Type", MediaType.APPLICATION_JSON_VALUE))
            .andRespond(
                withSuccess(
                    """
                    {
                      "candidates": [
                        {
                          "content": {
                            "parts": [
                              {
                                "text": "Your music taste is like a plain rice cake: technically food, but nobody is excited about it."
                              }
                            ],
                            "role": "model"
                          },
                          "finishReason": "STOP",
                          "index": 0
                        }
                      ]
                    }
                    """.trimIndent(),
                    MediaType.APPLICATION_JSON,
                ),
            )

        val roast = llmService.generateRoastFromData(data)

        assertEquals("Your music taste is like a plain rice cake: technically food, but nobody is excited about it.", roast)
        server.verify()
    }

    @Test
    fun `generateRoast calls userDataService and returns roast`() {
        val data = UserSpotifyDataResponse(
            profile = null,
            topTracks = emptyList(),
            topArtists = emptyList(),
            recentlyPlayed = emptyList()
        )
        every { userDataService.getSpotifyData(10, "medium_term") } returns data

        server.expect(once(), requestTo("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"))
            .andRespond(
                withSuccess(
                    """
                    {
                      "candidates": [
                        {
                          "content": {
                            "parts": [
                              {
                                "text": "Another roast"
                              }
                            ]
                          }
                        }
                      ]
                    }
                    """.trimIndent(),
                    MediaType.APPLICATION_JSON,
                ),
            )

        val roast = llmService.generateRoast(10, "medium_term")

        assertEquals("Another roast", roast)
        server.verify()
    }
}

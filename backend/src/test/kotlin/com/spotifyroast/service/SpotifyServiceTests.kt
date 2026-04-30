package com.spotifyroast.service

import com.spotifyroast.model.User
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.client.ExpectedCount.once
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import java.time.OffsetDateTime

class SpotifyServiceTests {

    private val builder = RestClient.builder().baseUrl("https://api.spotify.com/v1")
    private val server = MockRestServiceServer.bindTo(builder).build()
    private val spotifyService = SpotifyService(builder.build())

    @Test
    fun `fetches user profile with bearer token`() {
        server.expect(once(), requestTo("https://api.spotify.com/v1/me"))
            .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
            .andRespond(
                withSuccess(
                    """
                    {
                      "id": "spotify-user",
                      "display_name": "Alice",
                      "images": []
                    }
                    """.trimIndent(),
                    MediaType.APPLICATION_JSON,
                ),
            )

        val profile = spotifyService.getUserProfile(testUser())

        assertEquals("spotify-user", profile?.id)
        assertEquals("Alice", profile?.displayName)
        server.verify()
    }

    @Test
    fun `fetches top tracks with query params and bearer token`() {
        server.expect(
            once(),
            requestTo("https://api.spotify.com/v1/me/top/tracks?limit=2&time_range=short_term"),
        )
            .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
            .andRespond(
                withSuccess(
                    """
                    {
                      "items": [
                        {
                          "id": "track-1",
                          "name": "Song",
                          "artists": [{"id": "artist-1", "name": "Artist"}],
                          "album": {"id": "album-1", "name": "Album", "images": []},
                          "popularity": 90,
                          "duration_ms": 123000
                        }
                      ]
                    }
                    """.trimIndent(),
                    MediaType.APPLICATION_JSON,
                ),
            )

        val tracks = spotifyService.getTopTracks(testUser(), limit = 2, timeRange = "short_term")

        assertEquals("track-1", tracks?.items?.single()?.id)
        server.verify()
    }

    @Test
    fun `fetches top artists with query params and bearer token`() {
        server.expect(
            once(),
            requestTo("https://api.spotify.com/v1/me/top/artists?limit=2&time_range=long_term"),
        )
            .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
            .andRespond(
                withSuccess(
                    """
                    {
                      "items": [
                        {
                          "id": "artist-1",
                          "name": "Artist",
                          "genres": ["pop", "dance pop"],
                          "images": [],
                          "popularity": 80,
                          "followers": {"href": null, "total": 1234},
                          "external_urls": {"spotify": "https://open.spotify.com/artist/artist-1"}
                        }
                      ]
                    }
                    """.trimIndent(),
                    MediaType.APPLICATION_JSON,
                ),
            )

        val artists = spotifyService.getTopArtists(testUser(), limit = 2, timeRange = "long_term")

        val artist = artists?.items?.single()
        assertEquals("artist-1", artist?.id)
        assertEquals(listOf("pop", "dance pop"), artist?.genres)
        assertEquals(1234, artist?.followers?.total)
        server.verify()
    }

    @Test
    fun `fetches recently played tracks with query params and bearer token`() {
        server.expect(
            once(),
            requestTo("https://api.spotify.com/v1/me/player/recently-played?limit=1"),
        )
            .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
            .andRespond(
                withSuccess(
                    """
                    {
                      "items": [
                        {
                          "track": {
                            "id": "track-1",
                            "name": "Song",
                            "artists": [{"id": "artist-1", "name": "Artist"}],
                            "album": {"id": "album-1", "name": "Album", "images": []},
                            "duration_ms": 123000
                          },
                          "played_at": "2026-04-30T10:15:30Z",
                          "context": {
                            "type": "playlist",
                            "uri": "spotify:playlist:playlist-1"
                          }
                        }
                      ]
                    }
                    """.trimIndent(),
                    MediaType.APPLICATION_JSON,
                ),
            )

        val recentlyPlayed = spotifyService.getRecentlyPlayed(testUser(), limit = 1)

        val item = recentlyPlayed?.items?.single()
        assertEquals("track-1", item?.track?.id)
        assertEquals("2026-04-30T10:15:30Z", item?.playedAt)
        assertEquals("playlist", item?.context?.type)
        server.verify()
    }

    private fun testUser() = User(
        spotifyId = "spotify-user",
        displayName = "Alice",
        accessToken = "access-token",
        refreshToken = "refresh-token",
        tokenExpiresAt = OffsetDateTime.now().plusHours(1),
    )
}

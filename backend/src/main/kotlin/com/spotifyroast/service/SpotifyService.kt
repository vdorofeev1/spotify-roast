package com.spotifyroast.service

import com.spotifyroast.dto.*
import com.spotifyroast.model.User
import lombok.RequiredArgsConstructor
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.body

@Service
@RequiredArgsConstructor
class SpotifyService(
    @Qualifier("spotifyRestClient")
    private val spotifyRestClient: RestClient
) {

    fun getUserProfile(user: User): SpotifyUserProfile? {
        return spotifyRestClient.get()
            .uri("/me")
            .header("Authorization", "Bearer ${user.accessToken}")
            .retrieve()
            .body<SpotifyUserProfile>()
    }

    fun getTopTracks(user: User, limit: Int = 10, timeRange: String = "medium_term"): SpotifyTopTracksResponse? {
        return getTopItems(user, "tracks", limit, timeRange)
            .body<SpotifyTopTracksResponse>()
    }

    fun getTopArtists(user: User, limit: Int = 10, timeRange: String = "medium_term"): SpotifyTopArtistsResponse? {
        return getTopItems(user, "artists", limit, timeRange)
            .body<SpotifyTopArtistsResponse>()
    }

    fun getRecentlyPlayed(user: User, limit: Int = 50): SpotifyRecentlyPlayedResponse? {
        return spotifyRestClient.get()
            .uri { builder ->
                builder.path("/me/player/recently-played")
                    .queryParam("limit", limit)
                    .build()
            }
            .header("Authorization", "Bearer ${user.accessToken}")
            .retrieve()
            .body<SpotifyRecentlyPlayedResponse>()
    }

    private fun getTopItems(
        user: User,
        type: String,
        limit: Int,
        timeRange: String,
    ): RestClient.ResponseSpec {
        return spotifyRestClient.get()
            .uri { builder ->
                builder.path("/me/top/{type}")
                    .queryParam("limit", limit)
                    .queryParam("time_range", timeRange)
                    .build(type)
            }
            .header("Authorization", "Bearer ${user.accessToken}")
            .retrieve()
    }
}

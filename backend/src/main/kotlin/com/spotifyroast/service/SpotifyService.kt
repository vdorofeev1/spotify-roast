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

    fun getTopArtists(user: User, limit: Int = 10, timeRange: String = "medium_term"): SpotifyTopArtistsResponse? {
        return spotifyRestClient.get()
            .uri { builder ->
                builder.path("/me/top/artists")
                    .queryParam("limit", limit)
                    .queryParam("time_range", timeRange)
                    .build()
            }
            .header("Authorization", "Bearer ${user.accessToken}")
            .retrieve()
            .body<SpotifyTopArtistsResponse>()
    }

    fun getTopTracks(user: User, limit: Int = 10, timeRange: String = "medium_term"): SpotifyTopTracksResponse? {
        return spotifyRestClient.get()
            .uri { builder ->
                builder.path("/me/top/tracks")
                    .queryParam("limit", limit)
                    .queryParam("time_range", timeRange)
                    .build()
            }
            .header("Authorization", "Bearer ${user.accessToken}")
            .retrieve()
            .body<SpotifyTopTracksResponse>()
    }

    fun getAudioFeatures(user: User, trackIds: List<String>): SpotifyAudioFeaturesResponse? {
        if (trackIds.isEmpty()) return null
        return spotifyRestClient.get()
            .uri { builder ->
                builder.path("/audio-features")
                    .queryParam("ids", trackIds.joinToString(","))
                    .build()
            }
            .header("Authorization", "Bearer ${user.accessToken}")
            .retrieve()
            .body<SpotifyAudioFeaturesResponse>()
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

    /**
     * Aggregates all data needed for a roast.
     */
    fun getRoastData(user: User): RoastData {
        val topArtists = getTopArtists(user)
        val topTracks = getTopTracks(user)
        val recentlyPlayed = getRecentlyPlayed(user)
        
        val trackIds = topTracks?.items?.map { it.id } ?: emptyList()
        val audioFeatures = if (trackIds.isNotEmpty()) getAudioFeatures(user, trackIds) else null

        return RoastData(
            topArtists = topArtists,
            topTracks = topTracks,
            audioFeatures = audioFeatures,
            recentlyPlayed = recentlyPlayed
        )
    }
}

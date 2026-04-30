package com.spotifyroast.service

import com.spotifyroast.dto.UserProfileResponse
import com.spotifyroast.dto.UserRecentlyPlayedItemResponse
import com.spotifyroast.dto.UserRecentlyPlayedResponse
import com.spotifyroast.dto.UserSpotifyDataResponse
import com.spotifyroast.dto.UserTopArtistResponse
import com.spotifyroast.dto.UserTopArtistsResponse
import com.spotifyroast.dto.UserTopTrackResponse
import com.spotifyroast.dto.UserTopTracksResponse
import org.springframework.stereotype.Service

@Service
class UserDataService(
    private val currentUserService: CurrentUserService,
    private val spotifyAuthService: SpotifyAuthService,
    private val spotifyService: SpotifyService,
) {

    fun getTopTracks(limit: Int, timeRange: String): UserTopTracksResponse {
        validateLimit(limit)
        validateTimeRange(timeRange)

        val user = spotifyAuthService.refreshUserToken(currentUserService.getCurrentUser())
        val topTracks = spotifyService.getTopTracks(
            user = user,
            limit = limit,
            timeRange = timeRange,
        )

        return UserTopTracksResponse(
            items = topTracks?.items.orEmpty().map(UserTopTrackResponse::from),
        )
    }

    fun getTopArtists(limit: Int, timeRange: String): UserTopArtistsResponse {
        validateLimit(limit)
        validateTimeRange(timeRange)

        val user = spotifyAuthService.refreshUserToken(currentUserService.getCurrentUser())
        val topArtists = spotifyService.getTopArtists(
            user = user,
            limit = limit,
            timeRange = timeRange,
        )

        return UserTopArtistsResponse(
            items = topArtists?.items.orEmpty().map(UserTopArtistResponse::from),
        )
    }

    fun getRecentlyPlayed(limit: Int): UserRecentlyPlayedResponse {
        validateLimit(limit)

        val user = spotifyAuthService.refreshUserToken(currentUserService.getCurrentUser())
        val recentlyPlayed = spotifyService.getRecentlyPlayed(
            user = user,
            limit = limit,
        )

        return UserRecentlyPlayedResponse(
            items = recentlyPlayed?.items.orEmpty().map(UserRecentlyPlayedItemResponse::from),
        )
    }

    fun getSpotifyData(limit: Int, timeRange: String): UserSpotifyDataResponse {
        validateLimit(limit)
        validateTimeRange(timeRange)

        val user = spotifyAuthService.refreshUserToken(currentUserService.getCurrentUser())
        val profile = spotifyService.getUserProfile(user)
        val topTracks = spotifyService.getTopTracks(
            user = user,
            limit = limit,
            timeRange = timeRange,
        )
        val topArtists = spotifyService.getTopArtists(
            user = user,
            limit = limit,
            timeRange = timeRange,
        )
        val recentlyPlayed = spotifyService.getRecentlyPlayed(
            user = user,
            limit = limit,
        )

        return UserSpotifyDataResponse(
            profile = profile?.let(UserProfileResponse::from),
            topTracks = topTracks?.items.orEmpty().map(UserTopTrackResponse::from),
            topArtists = topArtists?.items.orEmpty().map(UserTopArtistResponse::from),
            recentlyPlayed = recentlyPlayed?.items.orEmpty().map(UserRecentlyPlayedItemResponse::from),
        )
    }

    private fun validateLimit(limit: Int) {
        require(limit in 1..50) { "limit must be between 1 and 50." }
    }

    private fun validateTimeRange(timeRange: String) {
        require(timeRange in SUPPORTED_TIME_RANGES) {
            "timeRange must be one of: ${SUPPORTED_TIME_RANGES.joinToString(", ")}."
        }
    }

    private companion object {
        val SUPPORTED_TIME_RANGES = setOf("short_term", "medium_term", "long_term")
    }
}

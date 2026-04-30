package com.spotifyroast.controller

import com.spotifyroast.dto.UserRecentlyPlayedResponse
import com.spotifyroast.dto.UserSpotifyDataResponse
import com.spotifyroast.dto.UserTopArtistsResponse
import com.spotifyroast.dto.UserTopTracksResponse
import com.spotifyroast.service.UserDataService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class UserDataController(
    private val userDataService: UserDataService,
) {

    @GetMapping("/me/top-tracks")
    fun getTopTracks(
        @RequestParam(defaultValue = "10") limit: Int,
        @RequestParam(defaultValue = "medium_term") timeRange: String,
    ): UserTopTracksResponse {
        return userDataService.getTopTracks(
            limit = limit,
            timeRange = timeRange,
        )
    }

    @GetMapping("/me/top-artists")
    fun getTopArtists(
        @RequestParam(defaultValue = "10") limit: Int,
        @RequestParam(defaultValue = "medium_term") timeRange: String,
    ): UserTopArtistsResponse {
        return userDataService.getTopArtists(
            limit = limit,
            timeRange = timeRange,
        )
    }

    @GetMapping("/me/recently-played")
    fun getRecentlyPlayed(
        @RequestParam(defaultValue = "50") limit: Int,
    ): UserRecentlyPlayedResponse {
        return userDataService.getRecentlyPlayed(limit = limit)
    }

    @GetMapping("/me/spotify-data")
    fun getSpotifyData(
        @RequestParam(defaultValue = "10") limit: Int,
        @RequestParam(defaultValue = "medium_term") timeRange: String,
    ): UserSpotifyDataResponse {
        return userDataService.getSpotifyData(
            limit = limit,
            timeRange = timeRange,
        )
    }
}

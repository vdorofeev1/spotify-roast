package com.spotifyroast.dto

data class RoastData(
    val topArtists: SpotifyTopArtistsResponse?,
    val topTracks: SpotifyTopTracksResponse?,
    val audioFeatures: SpotifyAudioFeaturesResponse?,
    val recentlyPlayed: SpotifyRecentlyPlayedResponse?,
)

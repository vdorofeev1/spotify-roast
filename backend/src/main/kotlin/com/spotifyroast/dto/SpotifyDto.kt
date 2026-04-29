package com.spotifyroast.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class SpotifyUserProfile(
    val id: String,
    @JsonProperty("display_name") val displayName: String?,
    val images: List<SpotifyImage>?
)

data class SpotifyImage(
    val url: String,
    val height: Int?,
    val width: Int?
)

data class SpotifyTopArtistsResponse(
    val items: List<SpotifyArtist>
)

data class SpotifyArtist(
    val id: String,
    val name: String,
    val genres: List<String>,
    val popularity: Int,
    val images: List<SpotifyImage>
)

data class SpotifyTopTracksResponse(
    val items: List<SpotifyTrack>
)

data class SpotifyTrack(
    val id: String,
    val name: String,
    val artists: List<SpotifyArtistSimple>,
    val album: SpotifyAlbumSimple,
    val popularity: Int,
    @JsonProperty("duration_ms") val durationMs: Long
)

data class SpotifyArtistSimple(
    val id: String,
    val name: String
)

data class SpotifyAlbumSimple(
    val id: String,
    val name: String,
    val images: List<SpotifyImage>
)

data class SpotifyAudioFeaturesResponse(
    @JsonProperty("audio_features") val audioFeatures: List<SpotifyAudioFeatures?>
)

data class SpotifyAudioFeatures(
    val id: String,
    val danceability: Float,
    val energy: Float,
    val key: Int,
    val loudness: Float,
    val mode: Int,
    val speechiness: Float,
    val acousticness: Float,
    val instrumentalness: Float,
    val liveness: Float,
    val valence: Float,
    val tempo: Float
)

data class SpotifyRecentlyPlayedResponse(
    val items: List<SpotifyPlayHistory>
)

data class SpotifyPlayHistory(
    val track: SpotifyTrack,
    @JsonProperty("played_at") val playedAt: String
)

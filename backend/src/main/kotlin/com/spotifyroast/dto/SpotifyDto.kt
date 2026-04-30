package com.spotifyroast.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class SpotifyUserProfile(
    val id: String,
    @JsonProperty("display_name") val displayName: String?,
    val email: String? = null,
    val country: String? = null,
    val product: String? = null,
    val images: List<SpotifyImage>? = null,
    @JsonProperty("external_urls") val externalUrls: SpotifyExternalUrls? = null,
    val followers: SpotifyFollowers? = null,
    val href: String? = null,
    val type: String? = null,
    val uri: String? = null,
    @JsonProperty("explicit_content") val explicitContent: SpotifyExplicitContent? = null,
)

data class SpotifyImage(
    val url: String,
    val height: Int?,
    val width: Int?
)

data class SpotifyExternalUrls(
    val spotify: String? = null,
)

data class SpotifyFollowers(
    val href: String? = null,
    val total: Int? = null,
)

data class SpotifyExplicitContent(
    @JsonProperty("filter_enabled") val filterEnabled: Boolean? = null,
    @JsonProperty("filter_locked") val filterLocked: Boolean? = null,
)

data class SpotifyTopTracksResponse(
    val items: List<SpotifyTrack>
)

data class SpotifyTopArtistsResponse(
    val items: List<SpotifyArtist>
)

data class SpotifyRecentlyPlayedResponse(
    val items: List<SpotifyPlayHistory>
)

data class SpotifyTrack(
    val id: String,
    val name: String,
    val artists: List<SpotifyArtistSimple>,
    val album: SpotifyAlbumSimple,
    val popularity: Int? = null,
    @JsonProperty("duration_ms") val durationMs: Long,
    @JsonProperty("external_urls") val externalUrls: SpotifyExternalUrls? = null,
    val href: String? = null,
    val type: String? = null,
    val uri: String? = null,
    val explicit: Boolean? = null,
    @JsonProperty("track_number") val trackNumber: Int? = null,
    @JsonProperty("disc_number") val discNumber: Int? = null,
    @JsonProperty("is_local") val isLocal: Boolean? = null,
)

data class SpotifyArtistSimple(
    val id: String,
    val name: String,
    @JsonProperty("external_urls") val externalUrls: SpotifyExternalUrls? = null,
    val href: String? = null,
    val type: String? = null,
    val uri: String? = null,
)

data class SpotifyArtist(
    val id: String,
    val name: String,
    val genres: List<String> = emptyList(),
    val images: List<SpotifyImage> = emptyList(),
    val popularity: Int? = null,
    @JsonProperty("external_urls") val externalUrls: SpotifyExternalUrls? = null,
    val followers: SpotifyFollowers? = null,
    val href: String? = null,
    val type: String? = null,
    val uri: String? = null,
)

data class SpotifyAlbumSimple(
    val id: String,
    val name: String,
    val images: List<SpotifyImage>,
    @JsonProperty("album_type") val albumType: String? = null,
    @JsonProperty("total_tracks") val totalTracks: Int? = null,
    @JsonProperty("release_date") val releaseDate: String? = null,
    @JsonProperty("release_date_precision") val releaseDatePrecision: String? = null,
    @JsonProperty("external_urls") val externalUrls: SpotifyExternalUrls? = null,
    val href: String? = null,
    val type: String? = null,
    val uri: String? = null,
)

data class SpotifyPlayHistory(
    val track: SpotifyTrack,
    @JsonProperty("played_at") val playedAt: String,
    val context: SpotifyPlayContext? = null,
)

data class SpotifyPlayContext(
    val type: String? = null,
    val href: String? = null,
    @JsonProperty("external_urls") val externalUrls: SpotifyExternalUrls? = null,
    val uri: String? = null,
)

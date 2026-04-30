package com.spotifyroast.dto

data class UserTopTracksResponse(
    val items: List<UserTopTrackResponse>,
)

data class UserTopArtistsResponse(
    val items: List<UserTopArtistResponse>,
)

data class UserRecentlyPlayedResponse(
    val items: List<UserRecentlyPlayedItemResponse>,
)

data class UserSpotifyDataResponse(
    val profile: UserProfileResponse?,
    val topTracks: List<UserTopTrackResponse>,
    val topArtists: List<UserTopArtistResponse>,
    val recentlyPlayed: List<UserRecentlyPlayedItemResponse>,
)

data class UserProfileResponse(
    val id: String,
    val displayName: String?,
    val email: String?,
    val country: String?,
    val product: String?,
    val images: List<SpotifyImage>,
    val externalUrls: SpotifyExternalUrls?,
    val followers: SpotifyFollowers?,
    val href: String?,
    val type: String?,
    val uri: String?,
    val explicitContent: SpotifyExplicitContent?,
) {
    companion object {
        fun from(profile: SpotifyUserProfile) = UserProfileResponse(
            id = profile.id,
            displayName = profile.displayName,
            email = profile.email,
            country = profile.country,
            product = profile.product,
            images = profile.images.orEmpty(),
            externalUrls = profile.externalUrls,
            followers = profile.followers,
            href = profile.href,
            type = profile.type,
            uri = profile.uri,
            explicitContent = profile.explicitContent,
        )
    }
}

data class UserTopTrackResponse(
    val id: String,
    val name: String,
    val artists: List<UserTopTrackArtistResponse>,
    val album: UserTopTrackAlbumResponse,
    val popularity: Int? = null,
    val durationMs: Long,
    val externalUrls: SpotifyExternalUrls?,
    val href: String?,
    val type: String?,
    val uri: String?,
    val explicit: Boolean?,
    val trackNumber: Int?,
    val discNumber: Int?,
    val isLocal: Boolean?,
) {
    companion object {
        fun from(track: SpotifyTrack) = UserTopTrackResponse(
            id = track.id,
            name = track.name,
            artists = track.artists.map { artist ->
                UserTopTrackArtistResponse(
                    id = artist.id,
                    name = artist.name,
                    externalUrls = artist.externalUrls,
                    href = artist.href,
                    type = artist.type,
                    uri = artist.uri,
                )
            },
            album = UserTopTrackAlbumResponse(
                id = track.album.id,
                name = track.album.name,
                images = track.album.images,
                albumType = track.album.albumType,
                totalTracks = track.album.totalTracks,
                releaseDate = track.album.releaseDate,
                releaseDatePrecision = track.album.releaseDatePrecision,
                externalUrls = track.album.externalUrls,
                href = track.album.href,
                type = track.album.type,
                uri = track.album.uri,
            ),
            popularity = track.popularity,
            durationMs = track.durationMs,
            externalUrls = track.externalUrls,
            href = track.href,
            type = track.type,
            uri = track.uri,
            explicit = track.explicit,
            trackNumber = track.trackNumber,
            discNumber = track.discNumber,
            isLocal = track.isLocal,
        )
    }
}

data class UserTopTrackArtistResponse(
    val id: String,
    val name: String,
    val externalUrls: SpotifyExternalUrls?,
    val href: String?,
    val type: String?,
    val uri: String?,
)

data class UserTopTrackAlbumResponse(
    val id: String,
    val name: String,
    val images: List<SpotifyImage>,
    val albumType: String?,
    val totalTracks: Int?,
    val releaseDate: String?,
    val releaseDatePrecision: String?,
    val externalUrls: SpotifyExternalUrls?,
    val href: String?,
    val type: String?,
    val uri: String?,
)

data class UserTopArtistResponse(
    val id: String,
    val name: String,
    val genres: List<String>,
    val images: List<SpotifyImage>,
    val popularity: Int?,
    val externalUrls: SpotifyExternalUrls?,
    val followers: SpotifyFollowers?,
    val href: String?,
    val type: String?,
    val uri: String?,
) {
    companion object {
        fun from(artist: SpotifyArtist) = UserTopArtistResponse(
            id = artist.id,
            name = artist.name,
            genres = artist.genres,
            images = artist.images,
            popularity = artist.popularity,
            externalUrls = artist.externalUrls,
            followers = artist.followers,
            href = artist.href,
            type = artist.type,
            uri = artist.uri,
        )
    }
}

data class UserRecentlyPlayedItemResponse(
    val track: UserTopTrackResponse,
    val playedAt: String,
    val context: SpotifyPlayContext?,
) {
    companion object {
        fun from(item: SpotifyPlayHistory) = UserRecentlyPlayedItemResponse(
            track = UserTopTrackResponse.from(item.track),
            playedAt = item.playedAt,
            context = item.context,
        )
    }
}

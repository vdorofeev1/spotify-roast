package com.spotifyroast.model

import jakarta.persistence.*
import java.time.OffsetDateTime

@Entity
@Table(name = "roast_results")
class RoastResult(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Column(name = "roast_text", nullable = false, columnDefinition = "text")
    val roastText: String,

    @Column(name = "top_artists_json", columnDefinition = "jsonb")
    val topArtistsJson: String?,

    @Column(name = "top_tracks_json", columnDefinition = "jsonb")
    val topTracksJson: String?,

    @Column(name = "audio_features_json", columnDefinition = "jsonb")
    val audioFeaturesJson: String?,

    @Column(name = "recently_played_json", columnDefinition = "jsonb")
    val recentlyPlayedJson: String?,

    @Column(name = "generated_at", nullable = false, updatable = false)
    val generatedAt: OffsetDateTime = OffsetDateTime.now(),
)

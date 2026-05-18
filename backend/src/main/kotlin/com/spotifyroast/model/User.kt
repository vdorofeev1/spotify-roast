package com.spotifyroast.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.spotifyroast.config.TokenEncryptionConverter
import jakarta.persistence.*
import java.time.OffsetDateTime

@Entity
@Table(name = "users")
class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "spotify_id", nullable = false, unique = true)
    val spotifyId: String,

    @Column(name = "display_name")
    val displayName: String?,

    @JsonIgnore
    @Convert(converter = TokenEncryptionConverter::class)
    @Column(name = "access_token", nullable = false, columnDefinition = "text")
    var accessToken: String,

    @JsonIgnore
    @Convert(converter = TokenEncryptionConverter::class)
    @Column(name = "refresh_token", nullable = false, columnDefinition = "text")
    var refreshToken: String,

    @JsonIgnore
    @Column(name = "token_expires_at", nullable = false)
    var tokenExpiresAt: OffsetDateTime,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
)

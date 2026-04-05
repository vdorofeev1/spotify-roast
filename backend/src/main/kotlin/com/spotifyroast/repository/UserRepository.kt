package com.spotifyroast.repository

import com.spotifyroast.model.User
import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<User, Long> {
    fun findBySpotifyId(spotifyId: String): User?
}

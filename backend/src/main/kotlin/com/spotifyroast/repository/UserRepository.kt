package com.spotifyroast.repository

import com.spotifyroast.model.User
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<User, Long> {
    fun findBySpotifyId(spotifyId: String): User?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.spotifyId = :spotifyId")
    fun findBySpotifyIdForUpdate(spotifyId: String): User?
}

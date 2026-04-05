package com.spotifyroast.repository

import com.spotifyroast.model.RoastResult
import com.spotifyroast.model.User
import org.springframework.data.jpa.repository.JpaRepository

interface RoastResultRepository : JpaRepository<RoastResult, Long> {
    fun findTopByUserOrderByGeneratedAtDesc(user: User): RoastResult?
}

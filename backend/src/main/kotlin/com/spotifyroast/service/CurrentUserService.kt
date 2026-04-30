package com.spotifyroast.service

import com.spotifyroast.model.User
import com.spotifyroast.repository.UserRepository
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Service

@Service
class CurrentUserService(
    private val userRepository: UserRepository,
) {

    fun getCurrentUser(): User {
        val principal = SecurityContextHolder.getContext().authentication?.principal as? OAuth2User
            ?: throw IllegalStateException("Authenticated Spotify user is required.")

        return userRepository.findBySpotifyId(principal.name)
            ?: throw IllegalStateException("User not found in database.")
    }
}

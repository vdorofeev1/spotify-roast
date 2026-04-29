package com.spotifyroast.service

import com.spotifyroast.model.User
import com.spotifyroast.repository.UserRepository
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Service

@Service
class RoastService(
    private val spotifyService: SpotifyService,
    private val spotifyAuthService: SpotifyAuthService,
    private val userRepository: UserRepository
) {

    fun generateRoast(): Map<String, Any?> {
        val user = getCurrentUser()
        val updatedUser = spotifyAuthService.refreshUserToken(user)
        
        return spotifyService.getRoastData(updatedUser)
    }

    private fun getCurrentUser(): User {
        val principal = SecurityContextHolder.getContext().authentication?.principal as OAuth2User
        val spotifyId = principal.name
        return userRepository.findBySpotifyId(spotifyId) 
            ?: throw RuntimeException("User not found in database")
    }
}

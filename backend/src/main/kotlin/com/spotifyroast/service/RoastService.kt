package com.spotifyroast.service

import com.spotifyroast.dto.RoastResponse
import com.spotifyroast.model.RoastResult
import com.spotifyroast.model.User
import com.spotifyroast.repository.RoastResultRepository
import com.spotifyroast.repository.UserRepository
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper

@Service
class RoastService(
    private val spotifyService: SpotifyService,
    private val spotifyAuthService: SpotifyAuthService,
    private val userRepository: UserRepository,
    private val roastResultRepository: RoastResultRepository,
    private val llmRoastClient: LlmRoastClient,
    private val objectMapper: ObjectMapper,
) {

    fun generateRoast(): RoastResponse {
        val user = getCurrentUser()
        val updatedUser = spotifyAuthService.refreshUserToken(user)
        val roastData = spotifyService.getRoastData(updatedUser)
        val roastText = llmRoastClient.generateRoast(roastData)

        roastResultRepository.save(
            RoastResult(
                user = updatedUser,
                roastText = roastText,
                topArtistsJson = toJsonOrNull(roastData.topArtists),
                topTracksJson = toJsonOrNull(roastData.topTracks),
                audioFeaturesJson = toJsonOrNull(roastData.audioFeatures),
                recentlyPlayedJson = toJsonOrNull(roastData.recentlyPlayed),
            ),
        )

        return RoastResponse(roastText = roastText)
    }

    private fun toJsonOrNull(value: Any?): String? {
        return value?.let(objectMapper::writeValueAsString)
    }

    private fun getCurrentUser(): User {
        val principal = SecurityContextHolder.getContext().authentication?.principal as OAuth2User
        val spotifyId = principal.name
        return userRepository.findBySpotifyId(spotifyId) 
            ?: throw RuntimeException("User not found in database")
    }
}

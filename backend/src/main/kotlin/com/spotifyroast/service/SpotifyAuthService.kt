package com.spotifyroast.service

import com.spotifyroast.model.User
import com.spotifyroast.repository.UserRepository
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import java.nio.charset.StandardCharsets
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.Base64

@Service
class SpotifyAuthService(
    private val userRepository: UserRepository,
    @Value("\${spring.security.oauth2.client.registration.spotify.client-id}") private val clientId: String,
    @Value("\${spring.security.oauth2.client.registration.spotify.client-secret}") private val clientSecret: String,
    @Qualifier("spotifyAccountsRestClient")
    private val restClient: RestClient,
) {

    @Transactional
    fun saveOrUpdateUser(oauth2User: OAuth2User, authorizedClient: OAuth2AuthorizedClient): User {
        val spotifyId = oauth2User.name // Default name attribute is "id" for Spotify
        val displayName = oauth2User.attributes["display_name"] as? String
        
        val accessToken = authorizedClient.accessToken.tokenValue
        val refreshToken = authorizedClient.refreshToken?.tokenValue
        val expiresAt = authorizedClient.accessToken.expiresAt?.atOffset(ZoneOffset.UTC) 
            ?: OffsetDateTime.now().plusSeconds(3600)

        val existingUser = userRepository.findBySpotifyId(spotifyId)
        
        return if (existingUser != null) {
            existingUser.accessToken = accessToken
            if (!refreshToken.isNullOrBlank()) {
                existingUser.refreshToken = refreshToken
            }
            existingUser.tokenExpiresAt = expiresAt
            //existingUser.displayName = displayName
            userRepository.save(existingUser)
        } else {
            require(!refreshToken.isNullOrBlank()) { "Spotify refresh token is required for new users." }

            val newUser = User(
                spotifyId = spotifyId,
                displayName = displayName,
                accessToken = accessToken,
                refreshToken = refreshToken,
                tokenExpiresAt = expiresAt
            )
            userRepository.save(newUser)
        }
    }

    @Transactional
    fun refreshUserToken(user: User): User {
        // Acquire a PESSIMISTIC_WRITE (SELECT FOR UPDATE) lock on the row so concurrent
        // calls for the same user are serialized at the DB level rather than racing to
        // call Spotify's /api/token with the same refresh token. After the lock is granted
        // re-read the row — another thread may have already refreshed it while we waited.
        val lockedUser = userRepository.findBySpotifyIdForUpdate(user.spotifyId)
            ?: throw IllegalStateException("User not found during token refresh.")
        if (!isTokenExpired(lockedUser)) return lockedUser

        val form = LinkedMultiValueMap<String, String>().apply {
            add("grant_type", "refresh_token")
            add("refresh_token", lockedUser.refreshToken)
        }

        val response = restClient.post()
            .uri("/api/token")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .header(
                "Authorization",
                "Basic " + Base64.getEncoder()
                    .encodeToString("$clientId:$clientSecret".toByteArray(StandardCharsets.UTF_8)),
            )
            .body(form)
            .retrieve()
            .body<Map<String, Any>>()

        val newAccessToken = response?.get("access_token") as? String
            ?: throw RuntimeException("Failed to refresh token")
        val expiresIn = (response["expires_in"] as? Number)?.toLong() ?: 3600
        val newRefreshToken = response["refresh_token"] as? String

        lockedUser.accessToken = newAccessToken
        lockedUser.tokenExpiresAt = OffsetDateTime.now(ZoneOffset.UTC).plusSeconds(expiresIn)
        if (newRefreshToken != null) {
            lockedUser.refreshToken = newRefreshToken
        }

        return userRepository.save(lockedUser)
    }

    fun isTokenExpired(user: User): Boolean {
        // Refresh if expiring in less than 1 minute
        return user.tokenExpiresAt.isBefore(OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(1))
    }
}

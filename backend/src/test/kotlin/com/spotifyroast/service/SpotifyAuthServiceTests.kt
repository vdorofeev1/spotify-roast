package com.spotifyroast.service

import com.spotifyroast.model.User
import com.spotifyroast.repository.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.OAuth2AccessToken
import org.springframework.security.oauth2.core.OAuth2RefreshToken
import org.springframework.security.oauth2.core.user.DefaultOAuth2User
import java.time.Instant
import java.time.OffsetDateTime

class SpotifyAuthServiceTests {

    private val userRepository = mock(UserRepository::class.java)
    private val spotifyAuthService = SpotifyAuthService(userRepository, "client-id", "client-secret")

    @Test
    fun `saves new user from oauth2 login`() {
        val oauth2User = oauth2User()
        val authorizedClient = authorizedClient()
        `when`(userRepository.findBySpotifyId("spotify-user")).thenReturn(null)
        `when`(userRepository.save(any(User::class.java))).thenAnswer { it.arguments[0] }

        val user = spotifyAuthService.saveOrUpdateUser(oauth2User, authorizedClient)

        assertEquals("spotify-user", user.spotifyId)
        assertEquals("Alice", user.displayName)
        assertEquals("access-token", user.accessToken)
        assertEquals("refresh-token", user.refreshToken)
        verify(userRepository).save(any(User::class.java))
    }

    @Test
    fun `updates existing user tokens from oauth2 login`() {
        val existingUser = User(
            spotifyId = "spotify-user",
            displayName = "Alice",
            accessToken = "old-access-token",
            refreshToken = "old-refresh-token",
            tokenExpiresAt = OffsetDateTime.now().minusHours(1),
        )
        `when`(userRepository.findBySpotifyId("spotify-user")).thenReturn(existingUser)
        `when`(userRepository.save(existingUser)).thenReturn(existingUser)

        val user = spotifyAuthService.saveOrUpdateUser(oauth2User(), authorizedClient())

        assertEquals("access-token", user.accessToken)
        assertEquals("refresh-token", user.refreshToken)
        verify(userRepository).save(existingUser)
    }

    private fun oauth2User() = DefaultOAuth2User(
        listOf(SimpleGrantedAuthority("ROLE_USER")),
        mapOf("id" to "spotify-user", "display_name" to "Alice"),
        "id",
    )

    private fun authorizedClient(): OAuth2AuthorizedClient {
        val issuedAt = Instant.parse("2026-01-01T00:00:00Z")
        val accessToken = OAuth2AccessToken(
            OAuth2AccessToken.TokenType.BEARER,
            "access-token",
            issuedAt,
            issuedAt.plusSeconds(3600),
        )
        val refreshToken = OAuth2RefreshToken("refresh-token", issuedAt)

        return OAuth2AuthorizedClient(clientRegistration(), "spotify-user", accessToken, refreshToken)
    }

    private fun clientRegistration() = ClientRegistration.withRegistrationId("spotify")
        .clientId("client-id")
        .clientSecret("client-secret")
        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
        .redirectUri("http://localhost/login/oauth2/code/spotify")
        .authorizationUri("https://accounts.spotify.com/authorize")
        .tokenUri("https://accounts.spotify.com/api/token")
        .scope("user-read-email")
        .build()
}

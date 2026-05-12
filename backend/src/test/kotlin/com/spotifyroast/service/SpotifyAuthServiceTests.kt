package com.spotifyroast.service

import com.spotifyroast.model.User
import com.spotifyroast.repository.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.OAuth2AccessToken
import org.springframework.security.oauth2.core.OAuth2RefreshToken
import org.springframework.security.oauth2.core.user.DefaultOAuth2User
import org.springframework.test.web.client.ExpectedCount.once
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.content
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import java.time.Instant
import java.time.OffsetDateTime

class SpotifyAuthServiceTests {

    private val userRepository = mock(UserRepository::class.java)
    private val accountsClientBuilder = RestClient.builder().baseUrl("https://accounts.spotify.com")
    private val server = MockRestServiceServer.bindTo(accountsClientBuilder).build()
    private val spotifyAuthService = SpotifyAuthService(
        userRepository,
        "client-id",
        "client-secret",
        accountsClientBuilder.build(),
    )

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

    @Test
    fun `preserves existing refresh token when oauth2 login does not include one`() {
        val existingUser = User(
            spotifyId = "spotify-user",
            displayName = "Alice",
            accessToken = "old-access-token",
            refreshToken = "old-refresh-token",
            tokenExpiresAt = OffsetDateTime.now().minusHours(1),
        )
        `when`(userRepository.findBySpotifyId("spotify-user")).thenReturn(existingUser)
        `when`(userRepository.save(existingUser)).thenReturn(existingUser)

        val user = spotifyAuthService.saveOrUpdateUser(oauth2User(), authorizedClient(refreshTokenValue = null))

        assertEquals("access-token", user.accessToken)
        assertEquals("old-refresh-token", user.refreshToken)
        verify(userRepository).save(existingUser)
    }

    @Test
    fun `rejects new user when oauth2 login does not include refresh token`() {
        `when`(userRepository.findBySpotifyId("spotify-user")).thenReturn(null)

        val exception = assertThrows(IllegalArgumentException::class.java) {
            spotifyAuthService.saveOrUpdateUser(oauth2User(), authorizedClient(refreshTokenValue = null))
        }

        assertEquals("Spotify refresh token is required for new users.", exception.message)
        verify(userRepository, never()).save(any(User::class.java))
    }

    @Test
    fun `refreshes expired user token with url encoded form body`() {
        val user = User(
            spotifyId = "spotify-user",
            displayName = "Alice",
            accessToken = "old-access-token",
            refreshToken = "abc+123&x=y%z",
            tokenExpiresAt = OffsetDateTime.now().minusHours(1),
        )
        `when`(userRepository.save(user)).thenReturn(user)

        server.expect(once(), requestTo("https://accounts.spotify.com/api/token"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header(HttpHeaders.AUTHORIZATION, "Basic Y2xpZW50LWlkOmNsaWVudC1zZWNyZXQ="))
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
            .andExpect(content().string("grant_type=refresh_token&refresh_token=abc%2B123%26x%3Dy%25z"))
            .andRespond(
                withSuccess(
                    """
                    {
                      "access_token": "new-access-token",
                      "expires_in": 3600,
                      "refresh_token": "new-refresh-token"
                    }
                    """.trimIndent(),
                    MediaType.APPLICATION_JSON,
                ),
            )

        val refreshedUser = spotifyAuthService.refreshUserToken(user)

        assertEquals("new-access-token", refreshedUser.accessToken)
        assertEquals("new-refresh-token", refreshedUser.refreshToken)
        verify(userRepository).save(user)
        server.verify()
    }

    private fun oauth2User() = DefaultOAuth2User(
        listOf(SimpleGrantedAuthority("ROLE_USER")),
        mapOf("id" to "spotify-user", "display_name" to "Alice"),
        "id",
    )

    private fun authorizedClient(refreshTokenValue: String? = "refresh-token"): OAuth2AuthorizedClient {
        val issuedAt = Instant.parse("2026-01-01T00:00:00Z")
        val accessToken = OAuth2AccessToken(
            OAuth2AccessToken.TokenType.BEARER,
            "access-token",
            issuedAt,
            issuedAt.plusSeconds(3600),
        )
        val refreshToken = refreshTokenValue?.let { OAuth2RefreshToken(it, issuedAt) }

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

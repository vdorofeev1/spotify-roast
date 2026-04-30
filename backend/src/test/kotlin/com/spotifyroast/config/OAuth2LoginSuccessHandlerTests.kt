package com.spotifyroast.config

import com.spotifyroast.service.SpotifyAuthService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.OAuth2AccessToken
import org.springframework.security.oauth2.core.OAuth2RefreshToken
import org.springframework.security.oauth2.core.user.DefaultOAuth2User
import java.time.Instant

class OAuth2LoginSuccessHandlerTests {

    private val spotifyAuthService = mock(SpotifyAuthService::class.java)
    private val authorizedClientService = mock(OAuth2AuthorizedClientService::class.java)
    private val handler = OAuth2LoginSuccessHandler(
        spotifyAuthService,
        authorizedClientService,
        "http://frontend.test",
    )

    @Test
    fun `saves logged in spotify user and redirects to top tracks page`() {
        val principal = DefaultOAuth2User(
            listOf(SimpleGrantedAuthority("ROLE_USER")),
            mapOf("id" to "spotify-user", "display_name" to "Alice"),
            "id",
        )
        val authentication = OAuth2AuthenticationToken(principal, principal.authorities, "spotify")
        val authorizedClient = authorizedClient()
        `when`(
            authorizedClientService.loadAuthorizedClient<OAuth2AuthorizedClient>(
                "spotify",
                "spotify-user",
            ),
        ).thenReturn(authorizedClient)

        val response = MockHttpServletResponse()
        handler.onAuthenticationSuccess(MockHttpServletRequest(), response, authentication)

        verify(spotifyAuthService).saveOrUpdateUser(principal, authorizedClient)
        assertEquals("http://frontend.test/top-tracks", response.redirectedUrl)
    }

    @Test
    fun `keeps loopback host consistent when redirecting to frontend`() {
        val handler = OAuth2LoginSuccessHandler(
            spotifyAuthService,
            authorizedClientService,
            "http://localhost:3000",
        )
        val principal = DefaultOAuth2User(
            listOf(SimpleGrantedAuthority("ROLE_USER")),
            mapOf("id" to "spotify-user", "display_name" to "Alice"),
            "id",
        )
        val authentication = OAuth2AuthenticationToken(principal, principal.authorities, "spotify")
        `when`(
            authorizedClientService.loadAuthorizedClient<OAuth2AuthorizedClient>(
                "spotify",
                "spotify-user",
            ),
        ).thenReturn(authorizedClient())

        val request = MockHttpServletRequest().apply {
            serverName = "127.0.0.1"
        }
        val response = MockHttpServletResponse()
        handler.onAuthenticationSuccess(request, response, authentication)

        assertEquals("http://127.0.0.1:3000/top-tracks", response.redirectedUrl)
    }

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

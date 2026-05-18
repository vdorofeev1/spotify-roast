package com.spotifyroast.config

import com.spotifyroast.service.SpotifyAuthService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
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

    private fun handler(frontendUrl: String = "http://frontend.test", allowedOrigins: String = "http://frontend.test") =
        OAuth2LoginSuccessHandler(spotifyAuthService, authorizedClientService, frontendUrl, allowedOrigins)

    private fun authenticate(handler: OAuth2LoginSuccessHandler, request: MockHttpServletRequest = MockHttpServletRequest()): MockHttpServletResponse {
        val principal = principal()
        val authentication = OAuth2AuthenticationToken(principal, principal.authorities, "spotify")
        `when`(authorizedClientService.loadAuthorizedClient<OAuth2AuthorizedClient>("spotify", "spotify-user"))
            .thenReturn(authorizedClient())
        val response = MockHttpServletResponse()
        handler.onAuthenticationSuccess(request, response, authentication)
        return response
    }

    // --- happy path ---

    @Test
    fun `saves logged in spotify user and redirects to roast page`() {
        val principal = principal()
        val authentication = OAuth2AuthenticationToken(principal, principal.authorities, "spotify")
        val authorizedClient = authorizedClient()
        `when`(authorizedClientService.loadAuthorizedClient<OAuth2AuthorizedClient>("spotify", "spotify-user"))
            .thenReturn(authorizedClient)

        val response = MockHttpServletResponse()
        handler().onAuthenticationSuccess(MockHttpServletRequest(), response, authentication)

        verify(spotifyAuthService).saveOrUpdateUser(principal, authorizedClient)
        assertEquals("http://frontend.test/roast", response.redirectedUrl)
    }

    @Test
    fun `redirects to same-origin roast page when frontend url is not configured`() {
        val response = authenticate(handler(frontendUrl = "", allowedOrigins = ""))
        assertEquals("/roast", response.redirectedUrl)
    }

    @Test
    fun `strips trailing slash from frontend url before appending roast path`() {
        val response = authenticate(handler(frontendUrl = "http://frontend.test/", allowedOrigins = "http://frontend.test"))
        assertEquals("http://frontend.test/roast", response.redirectedUrl)
    }

    // --- ALLOWED_ORIGINS validation ---

    @Test
    fun `rejects redirect when frontend url is not in allowed origins`() {
        val badHandler = handler(
            frontendUrl = "http://evil.example.com",
            allowedOrigins = "http://frontend.test",
        )
        assertThrows<IllegalStateException> {
            authenticate(badHandler)
        }
    }

    @Test
    fun `allows redirect when frontend url matches one of multiple allowed origins`() {
        val response = authenticate(
            handler(
                frontendUrl = "http://app.example.com",
                allowedOrigins = "http://frontend.test,http://app.example.com",
            )
        )
        assertEquals("http://app.example.com/roast", response.redirectedUrl)
    }

    @Test
    fun `skips origin validation when allowed origins is not configured`() {
        val response = authenticate(handler(frontendUrl = "http://frontend.test", allowedOrigins = ""))
        assertEquals("http://frontend.test/roast", response.redirectedUrl)
    }

    @Test
    fun `relative redirect is never subject to origin validation`() {
        // Empty frontendUrl produces a relative "/roast" — safe regardless of ALLOWED_ORIGINS.
        val response = authenticate(handler(frontendUrl = "", allowedOrigins = "http://frontend.test"))
        assertEquals("/roast", response.redirectedUrl)
    }

    @Test
    fun `Host header has no effect on redirect target`() {
        val request = MockHttpServletRequest().apply {
            addHeader("X-Forwarded-Host", "evil.attacker.com")
            serverName = "evil.attacker.com"
        }
        val response = authenticate(handler(), request)
        assertEquals("http://frontend.test/roast", response.redirectedUrl)
    }

    // --- helpers ---

    private fun principal() = DefaultOAuth2User(
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

package com.spotifyroast.config

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.core.user.OAuth2User

class RateLimitInterceptorTest {

    private val interceptor = RateLimitInterceptor(
        roastCooldownSeconds = 30,
        spotifyWindowSeconds = 60,
        spotifyMaxCalls = 3, // low limit to make the test fast
    )

    @BeforeEach
    fun setUpSecurityContext() {
        val principal = mockk<OAuth2User> { every { name } returns "user-spotify-123" }
        val auth = mockk<Authentication> { every { this@mockk.principal } returns principal }
        SecurityContextHolder.getContext().authentication = auth
    }

    // --- /api/roast cooldown ---

    @Test
    fun `first roast request is allowed`() {
        val allowed = interceptor.preHandle(request("/api/roast"), MockHttpServletResponse(), Any())
        assertTrue(allowed)
    }

    @Test
    fun `second roast request within cooldown is rejected with 429`() {
        interceptor.preHandle(request("/api/roast"), MockHttpServletResponse(), Any())

        val response = MockHttpServletResponse()
        val allowed = interceptor.preHandle(request("/api/roast"), response, Any())

        assertFalse(allowed)
        assertEquals(429, response.status)
    }

    @Test
    fun `rejected roast response includes Retry-After header`() {
        interceptor.preHandle(request("/api/roast"), MockHttpServletResponse(), Any())

        val response = MockHttpServletResponse()
        interceptor.preHandle(request("/api/roast"), response, Any())

        val retryAfter = response.getHeader("Retry-After")?.toLongOrNull()
        assertTrue(retryAfter != null && retryAfter in 1..30, "Retry-After should be 1–30, was $retryAfter")
    }

    @Test
    fun `different users have independent roast cooldowns`() {
        interceptor.preHandle(request("/api/roast"), MockHttpServletResponse(), Any())

        switchUser("user-spotify-456")
        val response = MockHttpServletResponse()
        val allowed = interceptor.preHandle(request("/api/roast"), response, Any())

        assertTrue(allowed)
        assertEquals(200, response.status)
    }

    // --- /api/me/* sliding window ---

    @Test
    fun `spotify data requests within limit are allowed`() {
        repeat(3) {
            val allowed = interceptor.preHandle(request("/api/me/top-tracks"), MockHttpServletResponse(), Any())
            assertTrue(allowed, "Call ${it + 1} should be allowed")
        }
    }

    @Test
    fun `spotify data request exceeding window limit is rejected`() {
        repeat(3) { interceptor.preHandle(request("/api/me/top-tracks"), MockHttpServletResponse(), Any()) }

        val response = MockHttpServletResponse()
        val allowed = interceptor.preHandle(request("/api/me/top-tracks"), response, Any())

        assertFalse(allowed)
        assertEquals(429, response.status)
    }

    @Test
    fun `rejected spotify data response includes Retry-After header`() {
        repeat(3) { interceptor.preHandle(request("/api/me/top-tracks"), MockHttpServletResponse(), Any()) }

        val response = MockHttpServletResponse()
        interceptor.preHandle(request("/api/me/top-tracks"), response, Any())

        val retryAfter = response.getHeader("Retry-After")?.toLongOrNull()
        assertTrue(retryAfter != null && retryAfter in 1..60, "Retry-After should be 1–60, was $retryAfter")
    }

    @Test
    fun `ping endpoint is not rate limited`() {
        repeat(10) {
            val allowed = interceptor.preHandle(request("/api/ping"), MockHttpServletResponse(), Any())
            assertTrue(allowed, "Ping call ${it + 1} should never be rate limited")
        }
    }

    @Test
    fun `unauthenticated request passes through rate limiter`() {
        SecurityContextHolder.clearContext()
        val allowed = interceptor.preHandle(request("/api/roast"), MockHttpServletResponse(), Any())
        assertTrue(allowed, "Rate limiter should pass unauthenticated requests to Spring Security")
    }

    // --- helpers ---

    private fun request(uri: String) = MockHttpServletRequest().apply { requestURI = uri }

    private fun switchUser(spotifyId: String) {
        val principal = mockk<OAuth2User> { every { name } returns spotifyId }
        val auth = mockk<Authentication> { every { this@mockk.principal } returns principal }
        SecurityContextHolder.getContext().authentication = auth
    }
}

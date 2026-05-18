package com.spotifyroast.config

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * Per-user rate limiter applied as a HandlerInterceptor.
 *
 * Two limits, keyed on the authenticated Spotify user ID:
 *  - roast endpoint    : minimum [roastCooldownSeconds] between calls (Spotify + Gemini, costs money)
 *  - Spotify endpoints : at most [spotifyMaxCalls] calls per [spotifyWindowSeconds] sliding window
 *
 * Returns HTTP 429 with a Retry-After header when a limit is exceeded.
 * All limits are configurable via application.yml under the rate-limit prefix.
 */
@Component
class RateLimitInterceptor(
    @Value("\${rate-limit.roast.cooldown-seconds:30}") private val roastCooldownSeconds: Long,
    @Value("\${rate-limit.spotify.window-seconds:60}") private val spotifyWindowSeconds: Long,
    @Value("\${rate-limit.spotify.max-calls:20}") private val spotifyMaxCalls: Int,
) : HandlerInterceptor, WebMvcConfigurer {

    private val log = LoggerFactory.getLogger(javaClass)

    // userId -> timestamp of the most recent /api/roast call
    private val roastLastCall = ConcurrentHashMap<String, Instant>()

    // userId -> ordered list of /api/me/* call timestamps within the current window
    private val spotifyCallLog = ConcurrentHashMap<String, ArrayDeque<Instant>>()

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(this)
            .addPathPatterns("/api/**")
            .excludePathPatterns("/api/ping")
    }

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        val userId = resolveUserId() ?: return true // unauthenticated — let Spring Security reject it

        return when {
            request.requestURI == "/api/roast" -> checkRoastCooldown(userId, response)
            request.requestURI.startsWith("/api/me/") ||
                request.requestURI == "/api/userdata" -> checkSpotifyWindow(userId, response)
            else -> true
        }
    }

    private fun checkRoastCooldown(userId: String, response: HttpServletResponse): Boolean {
        val now = Instant.now()
        val last = roastLastCall[userId]
        if (last != null) {
            val elapsed = now.epochSecond - last.epochSecond
            if (elapsed < roastCooldownSeconds) {
                val retryAfter = roastCooldownSeconds - elapsed
                log.debug("Rate limit hit: user={} endpoint=/api/roast retryAfter={}s", userId, retryAfter)
                reject(response, retryAfter, "Roast on cooldown — wait ${retryAfter}s before generating another.")
                return false
            }
        }
        roastLastCall[userId] = now
        return true
    }

    private fun checkSpotifyWindow(userId: String, response: HttpServletResponse): Boolean {
        val now = Instant.now()
        val windowStart = now.minusSeconds(spotifyWindowSeconds)
        val timestamps = spotifyCallLog.computeIfAbsent(userId) { ArrayDeque() }

        synchronized(timestamps) {
            // Evict timestamps that have fallen outside the sliding window
            while (timestamps.isNotEmpty() && timestamps.first().isBefore(windowStart)) {
                timestamps.removeFirst()
            }
            if (timestamps.size >= spotifyMaxCalls) {
                val retryAfter = spotifyWindowSeconds - (now.epochSecond - timestamps.first().epochSecond)
                log.debug(
                    "Rate limit hit: user={} endpoint=/api/me/* calls={} retryAfter={}s",
                    userId, timestamps.size, retryAfter
                )
                reject(response, retryAfter, "Too many requests — try again in ${retryAfter}s.")
                return false
            }
            timestamps.addLast(now)
        }
        return true
    }

    private fun resolveUserId(): String? =
        (SecurityContextHolder.getContext().authentication?.principal as? OAuth2User)?.name

    private fun reject(response: HttpServletResponse, retryAfterSeconds: Long, message: String) {
        response.status = HttpStatus.TOO_MANY_REQUESTS.value()
        response.setHeader("Retry-After", retryAfterSeconds.toString())
        response.contentType = "application/json;charset=UTF-8"
        response.writer.write("""{"message":"$message"}""")
    }
}

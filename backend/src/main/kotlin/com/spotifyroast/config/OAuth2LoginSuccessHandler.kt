package com.spotifyroast.config

import com.spotifyroast.service.SpotifyAuthService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler
import org.springframework.stereotype.Component

@Component
class OAuth2LoginSuccessHandler(
    private val spotifyAuthService: SpotifyAuthService,
    private val authorizedClientService: OAuth2AuthorizedClientService,
    @Value("\${frontend.url:}") private val frontendUrl: String,
    @Value("\${ALLOWED_ORIGINS:}") private val allowedOrigins: String,
) : SimpleUrlAuthenticationSuccessHandler() {

    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication
    ) {
        val oauthToken = authentication as OAuth2AuthenticationToken
        val authorizedClient = authorizedClientService.loadAuthorizedClient<org.springframework.security.oauth2.client.OAuth2AuthorizedClient>(
            oauthToken.authorizedClientRegistrationId,
            oauthToken.name
        )

        spotifyAuthService.saveOrUpdateUser(oauthToken.principal!!, authorizedClient)

        super.onAuthenticationSuccess(request, response, authentication)
    }

    override fun determineTargetUrl(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication?,
    ): String {
        val base = frontendUrl.trimEnd('/')
        // Empty frontendUrl → relative redirect, safe by definition (same origin).
        val target = if (base.isEmpty()) "/roast" else "$base/roast"

        if (!target.startsWith("/")) {
            validateAgainstAllowedOrigins(target)
        }

        return target
    }

    private fun validateAgainstAllowedOrigins(target: String) {
        val origins = allowedOrigins.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        if (origins.isEmpty()) return  // Not configured — skip; CORS will still reject cross-origin API calls.

        check(origins.any { target.startsWith(it) }) {
            "OAuth2 redirect target '$target' does not match any value in ALLOWED_ORIGINS. " +
                "Ensure FRONTEND_URL is listed in ALLOWED_ORIGINS."
        }
    }
}

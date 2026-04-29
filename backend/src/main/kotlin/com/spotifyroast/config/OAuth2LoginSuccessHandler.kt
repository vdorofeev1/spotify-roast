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
    @Value("\${FRONTEND_URL:http://localhost:3000}") private val frontendUrl: String
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

        defaultTargetUrl = "$frontendUrl/roast"
        super.onAuthenticationSuccess(request, response, authentication)
    }
}

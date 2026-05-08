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
import java.net.URI

@Component
class OAuth2LoginSuccessHandler(
    private val spotifyAuthService: SpotifyAuthService,
    private val authorizedClientService: OAuth2AuthorizedClientService,
    @Value("\${frontend.url:}") private val frontendUrl: String
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
        return "${resolveFrontendUrl(request)}/roast"
    }

    private fun resolveFrontendUrl(request: HttpServletRequest): String {
        val configuredUri = URI(frontendUrl.trimEnd('/'))
        val callbackHost = request.serverName
        val configuredHost = configuredUri.host

        val frontendUri = if (callbackHost.isLoopbackHost() && configuredHost.isLoopbackHost() && callbackHost != configuredHost) {
            URI(
                configuredUri.scheme,
                configuredUri.userInfo,
                callbackHost,
                configuredUri.port,
                configuredUri.path,
                configuredUri.query,
                configuredUri.fragment,
            )
        } else {
            configuredUri
        }

        return frontendUri.toString().trimEnd('/')
    }

    private fun String?.isLoopbackHost(): Boolean {
        return this == "localhost" || this == "127.0.0.1"
    }
}

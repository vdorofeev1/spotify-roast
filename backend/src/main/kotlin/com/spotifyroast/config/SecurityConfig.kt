package com.spotifyroast.config

import org.springframework.http.HttpStatus
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.authentication.HttpStatusEntryPoint
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.csrf.CookieCsrfTokenRepository
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher
import java.net.URI

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val oAuth2LoginSuccessHandler: OAuth2LoginSuccessHandler
) {

    @Value("\${ALLOWED_ORIGINS:http://localhost:3000}")
    private lateinit var allowedOrigins: String

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        return http
            .cors { cors ->
                cors.configurationSource {
                    val config = org.springframework.web.cors.CorsConfiguration()
                    config.allowedOrigins = expandLoopbackOrigins(allowedOrigins.split(","))
                    config.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
                    config.allowedHeaders = listOf("*")
                    config.allowCredentials = true
                    config
                }
            }
            .csrf { csrf ->
                csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                csrf.csrfTokenRequestHandler(CsrfTokenRequestAttributeHandler())
            }
            .authorizeHttpRequests { auth ->
                auth.requestMatchers("/", "/api/ping").permitAll()
                auth.anyRequest().authenticated()
            }
            .exceptionHandling { exceptions ->
                exceptions.defaultAuthenticationEntryPointFor(
                    HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                    PathPatternRequestMatcher.pathPattern("/api/**"),
                )
            }
            .oauth2Login { oauth2 -> oauth2
                .successHandler(oAuth2LoginSuccessHandler)
            }
            .build()
    }

    private fun expandLoopbackOrigins(origins: List<String>): List<String> {
        return origins
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .flatMap { origin ->
                val uri = runCatching { URI(origin) }.getOrNull()
                when (uri?.host) {
                    "localhost" -> listOf(origin, origin.replaceHost("127.0.0.1"))
                    "127.0.0.1" -> listOf(origin, origin.replaceHost("localhost"))
                    else -> listOf(origin)
                }
            }
            .distinct()
    }

    private fun String.replaceHost(host: String): String {
        val uri = URI(this)
        return URI(uri.scheme, uri.userInfo, host, uri.port, uri.path, uri.query, uri.fragment).toString()
    }
}

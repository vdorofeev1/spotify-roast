package com.spotifyroast.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.SecurityFilterChain

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
                    config.allowedOrigins = allowedOrigins.split(",")
                    config.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
                    config.allowedHeaders = listOf("*")
                    config.allowCredentials = true
                    config
                }
            }
            .csrf { csrf -> csrf.disable() }
            .authorizeHttpRequests { auth ->
                auth.requestMatchers("/").permitAll()
                auth.anyRequest().authenticated()
            }
            .oauth2Login { oauth2 -> oauth2
                .successHandler(oAuth2LoginSuccessHandler)
            }
            .build()
    }
}
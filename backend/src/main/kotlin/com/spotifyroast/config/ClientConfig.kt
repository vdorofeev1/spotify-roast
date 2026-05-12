package com.spotifyroast.config

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

@Configuration
class ClientConfig {

    @Bean
    @Qualifier("spotifyRestClient")
    fun spotifyRestClient(): RestClient {
        return RestClient.builder()
            .baseUrl("https://api.spotify.com/v1")
            .build()
    }

    @Bean
    @Qualifier("spotifyAccountsRestClient")
    fun spotifyAccountsRestClient(): RestClient {
        return RestClient.builder()
            .baseUrl("https://accounts.spotify.com")
            .build()
    }

    @Bean
    @Qualifier("geminiRestClient")
    fun geminiRestClient(): RestClient {
        return RestClient.builder()
            .baseUrl("https://generativelanguage.googleapis.com")
            .build()
    }
}

package com.spotifyroast.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

@Configuration
class ClientConfig {

    @Bean
    fun spotifyRestClient(): RestClient {
        return RestClient.builder()
            .baseUrl("https://api.spotify.com/v1")
            .build()
    }
}

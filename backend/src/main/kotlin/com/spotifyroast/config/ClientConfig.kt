package com.spotifyroast.config

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

@Configuration
@EnableConfigurationProperties(LlmProperties::class)
class ClientConfig {

    @Bean
    @Qualifier("spotifyRestClient")
    fun spotifyRestClient(): RestClient {
        return RestClient.builder()
            .baseUrl("https://api.spotify.com/v1")
            .build()
    }

    @Bean
    @Qualifier("llmRestClient")
    fun llmRestClient(llmProperties: LlmProperties): RestClient {
        return RestClient.builder()
            .baseUrl(llmProperties.baseUrl.trimEnd('/'))
            .build()
    }
}

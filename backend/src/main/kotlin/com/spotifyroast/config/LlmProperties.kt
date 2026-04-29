package com.spotifyroast.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "llm")
data class LlmProperties(
    val baseUrl: String = "https://generativelanguage.googleapis.com/v1beta",
    val apiKey: String,
    val model: String,
    val maxOutputTokens: Int = 900,
)

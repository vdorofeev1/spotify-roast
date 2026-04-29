package com.spotifyroast.service

import com.spotifyroast.config.LlmProperties
import com.spotifyroast.dto.RoastData
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.client.body
import tools.jackson.databind.ObjectMapper

@Service
class LlmRoastClient(
    @Qualifier("llmRestClient")
    private val llmRestClient: RestClient,
    private val llmProperties: LlmProperties,
    private val objectMapper: ObjectMapper,
) {

    fun generateRoast(roastData: RoastData): String {
        require(llmProperties.apiKey.isNotBlank()) {
            "LLM API key is not configured. Set LLM_API_KEY."
        }

        val request = mapOf(
            "contents" to listOf(
                mapOf(
                    "parts" to listOf(
                        mapOf("text" to buildPrompt(roastData))
                    )
                )
            ),
            "systemInstruction" to mapOf(
                "parts" to listOf(
                    mapOf("text" to ROAST_INSTRUCTIONS)
                )
            ),
            "generationConfig" to mapOf(
                "maxOutputTokens" to llmProperties.maxOutputTokens,
                "temperature" to 1.0
            )
        )

        val responseBody = try {
            llmRestClient.post()
                .uri("/models/${llmProperties.model}:generateContent")
                .header("x-goog-api-key", llmProperties.apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body<String>()
        } catch (ex: RestClientResponseException) {
            throw IllegalStateException(
                "LLM roast generation failed with ${ex.statusCode}: ${ex.responseBodyAsString}",
                ex,
            )
        }

        return responseBody
            ?.let(::extractRoastText)
            ?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("LLM returned no roast text")
    }

    private fun buildPrompt(roastData: RoastData): String {
        val roastDataJson = objectMapper.writeValueAsString(roastData)
        
        return """
            Generate a Spotify roast from this JSON data.
            
            Spotify data JSON:
            $roastDataJson
        """.trimIndent()
    }

    private fun extractRoastText(responseBody: String): String {
        val root = objectMapper.readTree(responseBody)
        
        // Handle Gemini error response
        val errorNode = root.path("error")
        if (!errorNode.isMissingNode) {
            val errorMessage = errorNode.path("message").asText("Unknown error")
            throw IllegalStateException("LLM roast generation failed: $errorMessage")
        }

        // Gemini response structure: candidates[0].content.parts[0].text
        val candidates = root.path("candidates")
        if (candidates.isArray && !candidates.isEmpty) {
            val firstCandidate = candidates.get(0)
            val text = firstCandidate.path("content").path("parts").get(0).path("text").asText("")
            if (text.isNotBlank()) {
                return text.trim()
            }
        }

        throw IllegalStateException("LLM returned no roast text or invalid structure: $responseBody")
    }

    private companion object {
        private val ROAST_INSTRUCTIONS = """
            You are the Spotify Roast Engine. Write a playful, sharp, specific roast based only on the provided Spotify data.
            Use real artists, tracks, genres, repeated patterns, and audio feature trends from the input.
            Keep it funny with using slurs, hate, harassment, body shaming, or attacks on protected traits.
            Do not invent listening data. If the data is thin, roast the lack of signal.
            Return only the roast text. Keep it between 120 and 220 words.
        """.trimIndent()
    }
}

package com.spotifyroast.controller

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.client.RestClientResponseException
import java.nio.charset.StandardCharsets

class ApiExceptionHandlerTests {

    private val handler = ApiExceptionHandler()

    @Test
    fun `service error response returns generic message and does not expose internal details`() {
        val exception = IllegalStateException("User not found in database.")

        val response = handler.handleServiceError(exception)

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
        assertEquals("An internal error occurred. Please try again later.", response.body?.message)
        assertFalse(response.body?.message.orEmpty().contains("User not found"))
        assertFalse(response.body?.message.orEmpty().contains("database"))
    }

    @Test
    fun `service error response is generic regardless of internal exception message`() {
        val exception = IllegalStateException("User not found during token refresh.")

        val response = handler.handleServiceError(exception)

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
        assertEquals("An internal error occurred. Please try again later.", response.body?.message)
        assertFalse(response.body?.message.orEmpty().contains("token refresh"))
    }

    @Test
    fun `upstream error response does not expose upstream body`() {
        val upstreamBody = """{"error":"invalid_grant","secret":"provider-token-detail"}"""
        val exception = RestClientResponseException(
            "Spotify token refresh failed",
            HttpStatus.BAD_REQUEST,
            "Bad Request",
            HttpHeaders.EMPTY,
            upstreamBody.toByteArray(StandardCharsets.UTF_8),
            StandardCharsets.UTF_8,
        )

        val response = handler.handleUpstreamError(exception)

        assertEquals(HttpStatus.BAD_GATEWAY, response.statusCode)
        assertNotNull(response.body)
        assertEquals("Upstream API request failed. Please try again later.", response.body?.message)
        assertFalse(response.body?.message.orEmpty().contains("invalid_grant"))
        assertFalse(response.body?.message.orEmpty().contains("provider-token-detail"))
    }
}

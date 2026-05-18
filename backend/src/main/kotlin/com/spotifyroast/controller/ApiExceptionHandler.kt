package com.spotifyroast.controller

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.client.RestClientResponseException

data class ApiErrorResponse(
    val message: String,
)

@RestControllerAdvice
class ApiExceptionHandler {

    private val logger = LoggerFactory.getLogger(ApiExceptionHandler::class.java)

    @ExceptionHandler(RestClientResponseException::class)
    fun handleUpstreamError(ex: RestClientResponseException): ResponseEntity<ApiErrorResponse> {
        logger.warn(
            "Upstream API failed with status {} and body: {}",
            ex.statusCode,
            ex.responseBodyAsString,
        )

        return ResponseEntity
            .status(HttpStatus.BAD_GATEWAY)
            .body(ApiErrorResponse("Upstream API request failed. Please try again later."))
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleBadRequest(ex: IllegalArgumentException): ResponseEntity<ApiErrorResponse> {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiErrorResponse(ex.message ?: "Invalid request."))
    }

    @ExceptionHandler(IllegalStateException::class)
    fun handleServiceError(ex: IllegalStateException): ResponseEntity<ApiErrorResponse> {
        logger.error("Internal service error: {}", ex.message, ex)
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiErrorResponse("An internal error occurred. Please try again later."))
    }
}

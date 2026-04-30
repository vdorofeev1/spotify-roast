package com.spotifyroast.controller

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

    @ExceptionHandler(RestClientResponseException::class)
    fun handleUpstreamError(ex: RestClientResponseException): ResponseEntity<ApiErrorResponse> {
        return ResponseEntity
            .status(HttpStatus.BAD_GATEWAY)
            .body(ApiErrorResponse("Upstream API failed with ${ex.statusCode}: ${ex.responseBodyAsString}"))
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleBadRequest(ex: IllegalArgumentException): ResponseEntity<ApiErrorResponse> {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiErrorResponse(ex.message ?: "Invalid request."))
    }

    @ExceptionHandler(IllegalStateException::class)
    fun handleServiceError(ex: IllegalStateException): ResponseEntity<ApiErrorResponse> {
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiErrorResponse(ex.message ?: "Request failed."))
    }
}

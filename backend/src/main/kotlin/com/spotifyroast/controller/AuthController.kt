package com.spotifyroast.controller

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URI

@RestController
@RequestMapping("/auth")
class AuthController(
    @Value("\${app.base-url}") private val baseUrl: String,
) {

    @GetMapping("/callback")
    fun callback(): ResponseEntity<Void> {
        val headers = HttpHeaders()
        headers.location = URI.create("$baseUrl/roast")
        return ResponseEntity(headers, HttpStatus.FOUND)
    }

    @GetMapping("/login")
    fun login() = "Login page"

    @GetMapping("/")
    fun roast() = "Home page"
}

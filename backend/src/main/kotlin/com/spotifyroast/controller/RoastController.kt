package com.spotifyroast.controller

import com.spotifyroast.service.LlmService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class RoastController(
    private val llmService: LlmService
) {

    @GetMapping("/roast")
    fun generateRoast(
        @RequestParam(defaultValue = "10") limit: Int,
        @RequestParam(defaultValue = "medium_term") timeRange: String
    ): RoastResponse {
        val roast = llmService.generateRoast(limit, timeRange)
        return RoastResponse(roast)
    }

    data class RoastResponse(val roast: String)
}

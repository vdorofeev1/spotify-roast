package com.spotifyroast.controller

import com.spotifyroast.dto.RoastResponse
import com.spotifyroast.service.RoastService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class RoastController(
    private val roastService: RoastService
) {

    @GetMapping("/roast")
    fun getRoast(): RoastResponse {
        return roastService.generateRoast()
    }
}

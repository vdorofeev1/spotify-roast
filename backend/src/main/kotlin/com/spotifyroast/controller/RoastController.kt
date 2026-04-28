package com.spotifyroast.controller

import com.spotifyroast.dto.RoastResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class RoastController {

    @GetMapping("/roast")
    fun getRoast(): RoastResponse {
        val mockedRoast = """
            Your music taste is like a budget airline: you think you're going somewhere cool, 
            but you're mostly just stuck in a cramped seat listening to white noise. 
            
            Why do you have so much Taylor Swift? Are you trying to manifest a breakup 
            or just really into bridge-building? And don't even get me started on the 
            Lo-Fi Hip Hop beats. We get it, you study. Or you're just pretending to.
            
            Overall, your Spotify profile is the sonic equivalent of unseasoned chicken. 
            It's functional, but nobody's asking for seconds.
        """.trimIndent()
        
        return RoastResponse(roastText = mockedRoast)
    }
}

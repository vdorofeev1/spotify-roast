package com.spotifyroast.controller

import com.spotifyroast.dto.UserSpotifyDataResponse
import com.spotifyroast.service.UserDataService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.web.bind.annotation.GetMapping
import kotlin.reflect.full.findAnnotation

class UserDataControllerTests {

    private val userDataService = mockk<UserDataService>()
    private val controller = UserDataController(userDataService)

    @Test
    fun `maps aggregate user data endpoint to requested path`() {
        val mapping = UserDataController::getUserData.findAnnotation<GetMapping>()

        assertTrue(mapping?.value?.contains("/userdata") == true)
    }

    @Test
    fun `returns aggregate user data from service`() {
        val response = UserSpotifyDataResponse(
            profile = null,
            topTracks = emptyList(),
            topArtists = emptyList(),
            recentlyPlayed = emptyList(),
        )
        every { userDataService.getUserData(10, "medium_term") } returns response

        val result = controller.getUserData(
            limit = 10,
            timeRange = "medium_term",
        )

        assertEquals(response, result)
        verify(exactly = 1) { userDataService.getUserData(10, "medium_term") }
    }
}

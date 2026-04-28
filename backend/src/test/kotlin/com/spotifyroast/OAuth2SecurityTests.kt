package com.spotifyroast

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.web.util.UriComponentsBuilder
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@SpringBootTest(
    properties = [
        "spring.autoconfigure.exclude=" +
            "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration," +
            "org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration," +
            "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration," +
            "org.springframework.boot.data.jpa.autoconfigure.JpaRepositoriesAutoConfiguration",
        "spring.security.oauth2.client.registration.spotify.client-id=test-client",
        "spring.security.oauth2.client.registration.spotify.client-secret=test-secret",
    ],
)
@AutoConfigureMockMvc
class OAuth2SecurityTests {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `login page exposes spotify authorization link`() {
        mockMvc.get("/login")
            .andExpect {
                status { isOk() }
                content {
                    string(org.hamcrest.Matchers.containsString("/oauth2/authorization/spotify"))
                }
            }
    }

    @Test
    fun `spotify authorization endpoint redirects with expected oauth parameters`() {
        val response = mockMvc.get("/oauth2/authorization/spotify")
            .andExpect {
                status { is3xxRedirection() }
            }
            .andReturn()
            .response

        val redirectUrl = response.getHeader(HttpHeaders.LOCATION)
        assertNotNull(redirectUrl)

        val uri = UriComponentsBuilder.fromUriString(redirectUrl).build(true).toUri()
        val query = UriComponentsBuilder.fromUri(uri).build(true).queryParams

        assertEquals("accounts.spotify.com", uri.host)
        assertEquals("/authorize", uri.path)
        assertEquals("code", query.getFirst("response_type"))
        assertEquals("test-client", query.getFirst("client_id"))
        assertEquals(
            "http://localhost/login/oauth2/code/spotify",
            query.getFirst("redirect_uri"),
        )
        assertEquals("user-read-private%20user-read-email", query.getFirst("scope"))
        assertTrue(query.getFirst("state").isNullOrBlank().not())
        assertEquals("S256", query.getFirst("code_challenge_method"))
        assertTrue(query.getFirst("code_challenge").isNullOrBlank().not())
    }

    @Test
    fun `protected endpoints redirect anonymous users to login`() {
        mockMvc.get("/api/roast")
            .andExpect {
                status { is3xxRedirection() }
                redirectedUrl("/oauth2/authorization/spotify")
            }
    }
}

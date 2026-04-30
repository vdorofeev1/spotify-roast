package com.spotifyroast

import com.spotifyroast.repository.UserRepository
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoBean

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
class BackendApplicationTests {

    @MockitoBean
    lateinit var userRepository: UserRepository

    @Test
    fun contextLoads() {
    }

}

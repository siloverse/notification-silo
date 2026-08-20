package io.github.siloverse.notification.security

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigurationTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Test
    fun `anonymous request is rejected with 401`() {
        mockMvc.get("/api/users/me").andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `authenticated request reaches the endpoint and sees its claims`() {
        mockMvc.get("/api/users/me") {
            with(
                jwt()
                    .jwt { it.subject("some-uuid").claim("preferred_username", "test_user") }
                    .authorities(SimpleGrantedAuthority("ROLE_CUSTOMER")))
        }.andExpect {
            status { isOk() }
            jsonPath("$.username") { value("test_user") }
            jsonPath("$.authorities[0]") { value("ROLE_CUSTOMER") }
        }
    }

    @Test
    fun `hello endpoint also requires authentication`() {
        mockMvc.get("/").andExpect { status { isUnauthorized() } }
    }
}
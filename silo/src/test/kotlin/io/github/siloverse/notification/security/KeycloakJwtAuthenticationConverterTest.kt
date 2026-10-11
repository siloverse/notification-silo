package io.github.siloverse.notification.security

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.security.oauth2.jwt.Jwt

class KeycloakJwtAuthenticationConverterTest {

    private val converter = KeycloakJwtAuthenticationConverter()

    private fun jwt(claims: Map<String, Any>): Jwt = Jwt.withTokenValue("token")
        .header("alg", "RS256")
        .claims { it.putAll(claims) }
        .subject("subject-uuid")
        .build()

    @Test
    fun `maps realm roles to ROLE_ authorities`() {
        val auth = converter.convert(
            jwt(
                mapOf(
                    "realm_access" to mapOf("roles" to listOf("customer")),
                    "preferred_username" to "test_user",
                )
            )
        )
        assertThat(auth.authorities.map { it.authority }).containsExactly("ROLE_CUSTOMER")
        assertThat(auth.name).isEqualTo("test_user")
    }

    @Test
    fun `token without realm_access yields no authorities and does not crash`() {
        val auth = converter.convert(jwt(emptyMap()))
        assertThat(auth.authorities).isEmpty()
    }

    @Test
    fun `service account token without preferred_username falls back to sub`() {
        val auth = converter.convert(
            jwt(
                mapOf(
                    "realm_access" to mapOf("roles" to listOf("system")),
                )
            )
        )
        assertThat(auth.name).isEqualTo("subject-uuid")
    }
}
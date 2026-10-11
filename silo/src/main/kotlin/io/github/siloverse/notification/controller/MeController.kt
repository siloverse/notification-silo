package io.github.siloverse.notification.controller

import org.springframework.security.core.Authentication
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/users")
class MeController {

    @GetMapping("/me")
    fun me(@AuthenticationPrincipal jwt: Jwt, authentication: Authentication) = mapOf(
        "sub" to jwt.subject,
        "username" to jwt.getClaimAsString("preferred_username"),
        "email" to jwt.getClaimAsString("email"),
        "authorities" to authentication.authorities.map { it.authority },
    )

}
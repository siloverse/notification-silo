package io.github.siloverse.notification.security

import org.springframework.core.convert.converter.Converter
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken

class KeycloakJwtAuthenticationConverter : Converter<Jwt, AbstractAuthenticationToken> {
    //                                     ^ the contract the filter accepts: Jwt in, Authentication out

    override fun convert(jwt: Jwt): AbstractAuthenticationToken {
        val realmAccess = jwt.claims["realm_access"] as? Map<*, *>          // the Keycloak-ism
        val roles = realmAccess?.get("roles") as? Collection<*> ?: emptyList<Any>()
        //   `as?` + fallback: a token with no realm_access (possible!) yields zero
        //   authorities, not an exception — auth must not crash on lean tokens

        val authorities = roles.map { SimpleGrantedAuthority("ROLE_$it".uppercase()) }
        //   ROLE_ prefix is the Spring convention hasRole() depends on:
        //   hasRole("CUSTOMER") secretly looks for authority "ROLE_CUSTOMER";
        //   realm roles are lowercase in Keycloak, uppercased here (platform convention)

        return JwtAuthenticationToken(
            jwt,
            authorities,
            jwt.getClaimAsString("preferred_username") ?: jwt.subject.toString()
        )
        //   the Authentication itself: principal = the Jwt (what /me unwraps),
        //   authorities = your mapping, name = username instead of the sub UUID (log readability)
    }
}
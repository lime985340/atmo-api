package lime.atmo.api.oauth

import lime.atmo.api.security.JwtService
import lime.atmo.api.security.TokenHash
import lime.atmo.application.port.UserPort
import lime.atmo.infra.jpa.repo.AppUserRepository
import lime.atmo.infra.jpa.repo.RefreshTokenRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

class OAuthServiceAuthorizeUrlTest {
    @Test
    fun `kakao authorize url requests only the permitted profile scope`() {
        val service = OAuthService(
            props = OAuthProps(
                providers = mapOf(
                    "kakao" to OAuthProviderProps(
                        clientId = "test-client",
                        clientSecret = "test-secret",
                        redirectUri = "https://example.test/dev/atmo/api/v1/auth/oauth/kakao/callback",
                        authUri = "https://kauth.kakao.com/oauth/authorize",
                        scope = "profile_nickname",
                    ),
                ),
            ),
            client = Mockito.mock(OAuthClient::class.java),
            users = Mockito.mock(UserPort::class.java),
            jwt = Mockito.mock(JwtService::class.java),
            tokenHash = Mockito.mock(TokenHash::class.java),
            refreshRepo = Mockito.mock(RefreshTokenRepository::class.java),
            userRepo = Mockito.mock(AppUserRepository::class.java),
        )

        val query = service.authorizeUrl("kakao", "state")
            .substringAfter('?')
            .split('&')
            .associate { parameter ->
                val (key, value) = parameter.split('=', limit = 2)
                URLDecoder.decode(key, StandardCharsets.UTF_8) to
                    URLDecoder.decode(value, StandardCharsets.UTF_8)
            }

        assertEquals("profile_nickname", query["scope"])
        assertEquals(
            "https://example.test/dev/atmo/api/v1/auth/oauth/kakao/callback",
            query["redirect_uri"],
        )
    }
}

package lime.atmos.api.controller

import lime.atmos.api.oauth.OAuthService
import lime.atmos.api.security.JwtService
import lime.atmos.api.security.TokenHash
import lime.atmos.infra.jpa.repo.RefreshTokenRepository
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.util.UUID

data class RefreshRequest(val refreshToken: String)

data class TokenResponse(val accessToken: String, val refreshToken: String)

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val oauth: OAuthService,
    private val jwt: JwtService,
    private val tokenHash: TokenHash,
    private val refreshRepo: RefreshTokenRepository
) {

    @GetMapping("/oauth/{provider}/authorize-url")
    fun authorizeUrl(@PathVariable provider: String): Map<String, String> {
        val state = UUID.randomUUID().toString()

        return mapOf(
            "authorizeUrl" to oauth.authorizeUrl(provider, state),
            "state" to state
        )
    }

    @GetMapping("/oauth/{provider}/callback")
    fun callback(
        @PathVariable provider: String,
        @RequestParam code: String,
        @RequestParam(required = false) state: String?,
        req: HttpServletRequest
    ): TokenResponse {
        val pair = oauth.loginWithCode(provider, code, req.getHeader("User-Agent"), req.remoteAddr)

        return TokenResponse(pair.accessToken, pair.refreshToken)
    }

    @PostMapping("/refresh")
    fun refresh(@RequestBody body: RefreshRequest): ResponseEntity<Any> {
        val userId = jwt.parseRefreshUserId(body.refreshToken)
            ?: return ResponseEntity.status(401).body(mapOf("error" to "invalid_refresh"))

        val hash = tokenHash.sha256Base64(body.refreshToken)
        val stored = refreshRepo.findByTokenHash(hash)
            ?: return ResponseEntity.status(401).body(mapOf("error" to "refresh_not_found"))

        if (stored.revokedAt != null) {
            return ResponseEntity.status(401).body(mapOf("error" to "refresh_revoked"))
        }

        if (stored.expiresAt.isBefore(Instant.now())) {
            return ResponseEntity.status(401).body(mapOf("error" to "refresh_expired"))
        }

        return ResponseEntity.ok(mapOf("accessToken" to jwt.createAccessToken(userId)))
    }

    @PostMapping("/logout")
    fun logout(@RequestBody body: RefreshRequest): ResponseEntity<Any> {
        val hash = tokenHash.sha256Base64(body.refreshToken)
        val stored = refreshRepo.findByTokenHash(hash) ?: return ResponseEntity.ok(mapOf("ok" to true))

        stored.revokedAt = Instant.now()
        refreshRepo.save(stored)

        return ResponseEntity.ok(mapOf("ok" to true))
    }
}

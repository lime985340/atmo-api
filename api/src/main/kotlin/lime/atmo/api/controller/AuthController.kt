package lime.atmo.api.controller

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import lime.atmo.api.oauth.OAuthService
import lime.atmo.api.security.JwtService
import lime.atmo.api.security.TokenHash
import lime.atmo.infra.jpa.entity.RefreshTokenEntity
import lime.atmo.infra.jpa.repo.RefreshTokenRepository
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.util.Base64
import java.util.UUID

data class RefreshRequest(val refreshToken: String? = null)

data class TokenResponse(val accessToken: String, val refreshToken: String)

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val oauth: OAuthService,
    private val jwt: JwtService,
    private val tokenHash: TokenHash,
    private val refreshRepo: RefreshTokenRepository,
) {

    @GetMapping("/oauth/{provider}/authorize-url")
    fun authorizeUrl(
        @PathVariable provider: String,
        @RequestParam(required = false) returnTo: String?,
    ): Map<String, String> {
        val state = UUID.randomUUID().toString()
        val encodedReturnTo = returnTo?.let {
            val encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(it.toByteArray())
            "$state.$encoded"
        } ?: state

        return mapOf(
            "authorizeUrl" to oauth.authorizeUrl(provider, encodedReturnTo),
            "state" to encodedReturnTo,
        )
    }

    @GetMapping("/oauth/{provider}/callback")
    fun callback(
        @PathVariable provider: String,
        @RequestParam code: String,
        @RequestParam(required = false) state: String?,
        req: HttpServletRequest,
        res: HttpServletResponse,
    ): ResponseEntity<Any> {
        val pair = oauth.loginWithCode(provider, code, req.getHeader("User-Agent"), req.remoteAddr)
        val returnTo = parseReturnTo(state)

        if (returnTo != null) {
            res.addHeader(
                "Set-Cookie",
                "refresh_token=${'$'}{pair.refreshToken}; HttpOnly; Secure; SameSite=Lax; Path=/api/v1/auth/refresh; Max-Age=${'$'}{60 * 60 * 24 * 30}",
            )
            val redirectUrl = if (returnTo.contains("#")) {
                "${'$'}returnTo&access_token=${'$'}{pair.accessToken}"
            } else {
                "${'$'}returnTo#access_token=${'$'}{pair.accessToken}"
            }
            return ResponseEntity.status(302).header("Location", redirectUrl).build()
        }

        return ResponseEntity.ok(TokenResponse(pair.accessToken, pair.refreshToken))
    }

    @PostMapping("/refresh")
    fun refresh(
        @RequestBody body: RefreshRequest,
        req: HttpServletRequest,
        res: HttpServletResponse,
        @RequestHeader(name = "X-Client", required = false, defaultValue = "app") client: String,
    ): ResponseEntity<Any> {
        val refreshToken = body.refreshToken?.ifBlank { null }
            ?: req.cookies?.firstOrNull { it.name == "refresh_token" }?.value
        if (refreshToken.isNullOrBlank()) {
            return ResponseEntity.status(401).body(mapOf("error" to "refresh_missing"))
        }

        val userId = jwt.parseRefreshUserId(refreshToken)
            ?: return ResponseEntity.status(401).body(mapOf("error" to "invalid_refresh"))

        val hash = tokenHash.sha256Base64(refreshToken)
        val stored = refreshRepo.findByTokenHash(hash)
            ?: return ResponseEntity.status(401).body(mapOf("error" to "refresh_not_found"))

        if (stored.revokedAt != null) {
            return ResponseEntity.status(401).body(mapOf("error" to "refresh_revoked"))
        }

        if (stored.expiresAt.isBefore(Instant.now())) {
            return ResponseEntity.status(401).body(mapOf("error" to "refresh_expired"))
        }

        // Rotate refresh token on every successful refresh.
        stored.revokedAt = Instant.now()
        refreshRepo.save(stored)

        val newRefresh = jwt.createRefreshToken(userId)
        val newHash = tokenHash.sha256Base64(newRefresh)

        // Keep expiry aligned with the refresh JWT TTL in JwtService (and OAuthService convention: 30d).
        val newEntity = RefreshTokenEntity(
            id = UUID.randomUUID(),
            user = stored.user,
            tokenHash = newHash,
            issuedAt = Instant.now(),
            expiresAt = Instant.now().plusSeconds(60L * 60 * 24 * 30),
            userAgent = req.getHeader("User-Agent"),
            ip = req.remoteAddr,
        )
        refreshRepo.save(newEntity)

        val access = jwt.createAccessToken(userId)

        return if (client.lowercase() == "web") {
            // Web: refresh token is stored only as HttpOnly cookie.
            // Path is restricted to refresh endpoint to reduce exposure.
            res.addHeader(
                "Set-Cookie",
                "refresh_token=${'$'}newRefresh; HttpOnly; Secure; SameSite=Lax; Path=/api/v1/auth/refresh; Max-Age=${'$'}{60 * 60 * 24 * 30}",
            )
            ResponseEntity.ok(mapOf("accessToken" to access))
        } else {
            // App: return refresh token in body.
            ResponseEntity.ok(TokenResponse(accessToken = access, refreshToken = newRefresh))
        }
    }

    @PostMapping("/logout")
    fun logout(@RequestBody body: RefreshRequest, req: HttpServletRequest): ResponseEntity<Any> {
        val refreshToken = body.refreshToken?.ifBlank { null }
            ?: req.cookies?.firstOrNull { it.name == "refresh_token" }?.value
        if (refreshToken.isNullOrBlank()) {
            return ResponseEntity.ok(mapOf("ok" to true))
        }

        val hash = tokenHash.sha256Base64(refreshToken)
        val stored = refreshRepo.findByTokenHash(hash) ?: return ResponseEntity.ok(mapOf("ok" to true))

        stored.revokedAt = Instant.now()
        refreshRepo.save(stored)

        return ResponseEntity.ok(mapOf("ok" to true))
    }

    private fun parseReturnTo(state: String?): String? {
        if (state.isNullOrBlank()) return null
        val parts = state.split(".")
        if (parts.size < 2) return null
        return runCatching {
            String(Base64.getUrlDecoder().decode(parts.last()))
        }.getOrNull()
    }
}

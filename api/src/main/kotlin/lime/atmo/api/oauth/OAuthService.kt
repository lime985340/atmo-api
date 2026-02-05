package lime.atmo.api.oauth

import lime.atmo.api.security.JwtService
import lime.atmo.api.security.TokenHash
import lime.atmo.application.port.UserPort
import lime.atmo.infra.jpa.entity.RefreshTokenEntity
import lime.atmo.infra.jpa.repo.AppUserRepository
import lime.atmo.infra.jpa.repo.RefreshTokenRepository
import org.springframework.stereotype.Service
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.UUID

data class TokenPair(
    val accessToken: String,
    val refreshToken: String,
)

@Service
class OAuthService(
    private val props: OAuthProps,
    private val client: OAuthClient,
    private val users: UserPort,
    private val jwt: JwtService,
    private val tokenHash: TokenHash,
    private val refreshRepo: RefreshTokenRepository,
    private val userRepo: AppUserRepository,
) {
    fun authorizeUrl(providerKey: String, state: String): String {
        val p = provider(providerKey)
        fun enc(s: String) = URLEncoder.encode(s, StandardCharsets.UTF_8)

        return buildString {
            append(p.authUri)
            append("?response_type=code")
            append("&client_id=").append(enc(p.clientId))
            append("&redirect_uri=").append(enc(p.redirectUri))
            append("&scope=").append(enc(p.scope))
            append("&state=").append(enc(state))
        }
    }

    fun loginWithCode(
        providerKey: String,
        code: String,
        userAgent: String?,
        ip: String?,
    ): TokenPair {
        val p = provider(providerKey)

        val tokenJson = client.exchangeCodeForToken(p, code).block()
            ?: throw IllegalStateException("token exchange failed")

        val accessToken = tokenJson["access_token"]?.asText()
            ?: throw IllegalStateException("access_token missing")

        val userJson = client.fetchUserInfo(p, accessToken).block()
            ?: throw IllegalStateException("userinfo failed")

        val (subject, email, name) = mapUser(providerKey, userJson)
        val user = users.findOrCreateByProvider(providerKey.uppercase(), subject, email, name)

        val ourAccess = jwt.createAccessToken(user.id)
        val ourRefresh = jwt.createRefreshToken(user.id)
        val hash = tokenHash.sha256Base64(ourRefresh)

        val userEntity = userRepo.findById(user.id).orElseThrow()
        refreshRepo.save(
            RefreshTokenEntity(
                id = UUID.randomUUID(),
                user = userEntity,
                tokenHash = hash,
                issuedAt = Instant.now(),
                expiresAt = Instant.now().plusSeconds(60L * 60 * 24 * 30),
                userAgent = userAgent,
                ip = ip,
            ),
        )
        return TokenPair(ourAccess, ourRefresh)
    }

    private fun provider(key: String) =
        props.providers[key.lowercase()] ?: throw IllegalArgumentException("unknown provider: $key")

    private fun mapUser(
        providerKey: String,
        json: com.fasterxml.jackson.databind.JsonNode,
    ): Triple<String, String?, String?> = when (providerKey.lowercase()) {
        "google" -> Triple(
            json["sub"]?.asText() ?: error("google sub missing"),
            json["email"]?.asText(),
            json["name"]?.asText(),
        )

        "naver" -> {
            val res = json["response"] ?: error("naver response missing")
            Triple(
                res["id"]?.asText() ?: error("naver id missing"),
                res["email"]?.asText(),
                res["name"]?.asText(),
            )
        }

        "kakao" -> {
            val id = json["id"]?.asText() ?: error("kakao id missing")
            val account = json["kakao_account"]
            val email = account?.get("email")?.asText()
            val nickname = json["properties"]?.get("nickname")?.asText()
                ?: account?.get("profile")?.get("nickname")?.asText()
            Triple(id, email, nickname)
        }

        else -> error("unsupported provider: $providerKey")
    }
}

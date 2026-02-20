package lime.atmo.api.oauth

import lime.atmo.api.exception.BadRequestException
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

        // URLEncoder encodes spaces as '+', which is not always interpreted as space
        // in query strings by OAuth providers. Encode spaces as %20 for compatibility.
        fun enc(s: String) = URLEncoder.encode(s, StandardCharsets.UTF_8).replace("+", "%20")

        return buildString {
            append(p.authUri)
            append("?response_type=code")
            append("&client_id=").append(enc(p.clientId))
            append("&redirect_uri=").append(enc(p.redirectUri))
            if (p.scope.isNotBlank()) {
                append("&scope=").append(enc(p.scope))
            }
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
            ?: throw BadRequestException("token exchange failed")

        val accessToken = tokenJson["access_token"]?.asText()
            ?: throw BadRequestException("access_token missing")

        val userJson = client.fetchUserInfo(providerKey, p, accessToken).block()
            ?: throw BadRequestException("userinfo failed")

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
        props.providers[key.lowercase()] ?: throw BadRequestException("unknown provider: $key")

    private fun mapUser(
        providerKey: String,
        json: com.fasterxml.jackson.databind.JsonNode,
    ): Triple<String, String?, String?> = when (providerKey.lowercase()) {
        "google" -> {
            val given = json["given_name"]?.asText()?.trim().orEmpty()
            val family = json["family_name"]?.asText()?.trim().orEmpty()
            val composed = listOf(family, given).filter { it.isNotBlank() }.joinToString(" ")
            val name = json["name"]?.asText() ?: composed.ifBlank { null }
            Triple(
                json["sub"]?.asText() ?: error("google sub missing"),
                json["email"]?.asText(),
                name,
            )
        }

        "naver" -> {
            val res = json["response"] ?: error("naver response missing")
            val name = res["name"]?.asText() ?: res["nickname"]?.asText()
            Triple(
                res["id"]?.asText() ?: error("naver id missing"),
                res["email"]?.asText(),
                name,
            )
        }

        "kakao" -> {
            val id = json["id"]?.asText() ?: error("kakao id missing")
            val account = json["kakao_account"]
            val email = account?.get("email")?.asText()
            val nickname = account?.get("profile")?.get("nickname")?.asText()
                ?: json["properties"]?.get("nickname")?.asText()
            Triple(id, email, nickname)
        }

        else -> error("unsupported provider: $providerKey")
    }
}

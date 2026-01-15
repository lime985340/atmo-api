package lime.atmos.api.security

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.Date
import java.util.UUID

@Service
class JwtService(private val props: JwtProps) {
    private val log = LoggerFactory.getLogger(JwtService::class.java)

    private fun key(secret: String) = Keys.hmacShaKeyFor(secret.toByteArray(StandardCharsets.UTF_8))

    fun createAccessToken(userId: UUID): String {
        val now = Instant.now()
        val exp = now.plusSeconds(props.accessTtlSeconds)
        return Jwts.builder()
            .issuer(props.issuer)
            .subject(userId.toString())
            .issuedAt(Date.from(now))
            .expiration(Date.from(exp))
            .claim("typ", "access")
            .signWith(key(props.accessSecret))
            .compact()
    }

    fun createRefreshToken(userId: UUID): String {
        val now = Instant.now()
        val exp = now.plusSeconds(props.refreshTtlSeconds)
        return Jwts.builder()
            .issuer(props.issuer)
            .subject(userId.toString())
            .issuedAt(Date.from(now))
            .expiration(Date.from(exp))
            .claim("typ", "refresh")
            .signWith(key(props.refreshSecret))
            .compact()
    }

    fun parseAccessUserId(token: String): UUID? = parse(token, props.accessSecret, "access")
    fun parseRefreshUserId(token: String): UUID? = parse(token, props.refreshSecret, "refresh")

    private fun parse(token: String, secret: String, typ: String): UUID? = try {
        val claims = Jwts.parser()
            .verifyWith(key(secret))
            .build()
            .parseSignedClaims(token)
            .payload

        if (claims["typ"] != typ) null else UUID.fromString(claims.subject)
    } catch (e: Exception) {
        log.debug("JWT parse failed: {}", e.message)
        null
    }
}

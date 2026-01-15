package lime.atmos.api.security

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated
import jakarta.validation.constraints.NotBlank

@ConfigurationProperties(prefix = "app.jwt")
@Validated
data class JwtProps(
    val issuer: String,
    @field:NotBlank val accessSecret: String,
    @field:NotBlank val refreshSecret: String,
    val accessTtlSeconds: Long,
    val refreshTtlSeconds: Long
)

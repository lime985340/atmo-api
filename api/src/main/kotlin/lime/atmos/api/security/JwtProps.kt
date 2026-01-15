package lime.atmos.api.security

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.validation.annotation.Validated
import javax.validation.constraints.NotBlank

@ConstructorBinding
@ConfigurationProperties(prefix = "app.jwt")
@Validated
data class JwtProps(
    val issuer: String,
    @field:NotBlank val accessSecret: String,
    @field:NotBlank val refreshSecret: String,
    val accessTtlSeconds: Long,
    val refreshTtlSeconds: Long
)

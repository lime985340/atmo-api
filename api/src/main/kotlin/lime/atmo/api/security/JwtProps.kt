package lime.atmo.api.security

import jakarta.validation.constraints.NotBlank
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

/**
 * JWT 설정 Properties
 *
 * application.yml의 app.jwt 설정을 바인딩합니다.
 */
@ConfigurationProperties(prefix = "app.jwt")
@Validated
data class JwtProps(
    @field:NotBlank
    val accessSecret: String,

    @field:NotBlank
    val refreshSecret: String,

    val issuer: String = "atmos",

    val accessTtlSeconds: Long = 900, // 15분

    val refreshTtlSeconds: Long = 2592000, // 30일
)

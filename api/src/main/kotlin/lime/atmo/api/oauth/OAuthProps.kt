package lime.atmo.api.oauth

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * OAuth 설정 Properties
 *
 * application.yml의 app.oauth 설정을 바인딩합니다.
 */
@ConfigurationProperties(prefix = "app.oauth")
data class OAuthProps(
    val providers: Map<String, OAuthProviderProps>,
)

/**
 * OAuth Provider 개별 설정
 *
 * 필수 필드만 포함하며, 추가 URI들은 각 Provider 구현체에서 하드코딩합니다.
 */
data class OAuthProviderProps(
    val clientId: String,
    val clientSecret: String,
    val redirectUri: String,
    val authUri: String = "",
    val tokenUri: String = "",
    val userinfoUri: String = "",
    val scope: String = "",
)

package lime.atmos.api.oauth

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.oauth")
data class OAuthProps(
    val providers: Map<String, OAuthProviderProps>
)

data class OAuthProviderProps(
    val clientId: String,
    val clientSecret: String,
    val redirectUri: String,
    val scope: String,
    val authUri: String,
    val tokenUri: String,
    val userinfoUri: String
)

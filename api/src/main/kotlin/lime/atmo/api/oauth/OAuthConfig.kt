package lime.atmo.api.oauth
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(OAuthProps::class)
class OAuthConfig

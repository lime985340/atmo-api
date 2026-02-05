package lime.atmo.common.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(PublicDataProps::class, ForecastRetentionProps::class)
class PublicDataConfig

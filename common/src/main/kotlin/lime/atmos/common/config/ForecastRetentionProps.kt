package lime.atmos.common.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.forecast")
data class ForecastRetentionProps(
    val retentionDays: Long = 2,
)

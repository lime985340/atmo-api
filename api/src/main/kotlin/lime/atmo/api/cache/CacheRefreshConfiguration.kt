package lime.atmo.api.cache

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor

@Configuration
class CacheRefreshConfiguration {
    @Bean("staleCacheRefreshExecutor")
    fun staleCacheRefreshExecutor(): ThreadPoolTaskExecutor = ThreadPoolTaskExecutor().apply {
        corePoolSize = 1
        maxPoolSize = 2
        queueCapacity = 32
        setThreadNamePrefix("stale-cache-refresh-")
    }
}

package lime.atmo.api.cache

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.core.type.TypeReference
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.dao.DataAccessException
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant
import java.util.concurrent.Executor

/**
 * Redis cache for public reads that remain useful when the database is asleep or unavailable.
 *
 * Fresh entries are returned directly. Expired-but-retained entries are returned immediately
 * while one bounded background refresh attempts to replace them. A cache miss still loads from
 * the source of truth synchronously.
 */
@Component
class StaleRedisReadCache(
    private val redis: StringRedisTemplate,
    private val objectMapper: ObjectMapper,
    @Qualifier("staleCacheRefreshExecutor") private val refreshExecutor: Executor,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    data class Read<T>(
        val value: T,
        val age: Duration,
        val servedStale: Boolean,
    )

    data class Envelope(
        val cachedAt: Instant,
        val payload: JsonNode,
    )

    fun <T : Any> getOrLoad(
        key: String,
        type: TypeReference<T>,
        freshFor: Duration,
        retainFor: Duration,
        loader: () -> T,
    ): Read<T> {
        require(!freshFor.isNegative && !freshFor.isZero) { "freshFor must be positive" }
        require(retainFor >= freshFor) { "retainFor must be at least freshFor" }

        val cached = try {
            redis.opsForValue().get(key)
        } catch (e: DataAccessException) {
            log.warn("Redis read failed for public cache key={}; loading from source", key, e)
            null
        }

        if (cached != null) {
            val envelope = decode(cached, key)
            if (envelope != null) {
                val age = Duration.between(envelope.cachedAt, Instant.now()).coerceAtLeast(Duration.ZERO)
                val value = runCatching { objectMapper.convertValue(envelope.payload, type) }
                    .onFailure { log.warn("Invalid Redis cache payload for key={}; loading from source", key, it) }
                    .getOrNull()
                if (value != null) {
                    if (age <= freshFor) return Read(value, age, servedStale = false)
                    if (age <= retainFor) {
                        scheduleRefresh(key, retainFor, loader)
                        return Read(value, age, servedStale = true)
                    }
                }
            }
        }

        val value = loader()
        write(key, value, retainFor)
        return Read(value, Duration.ZERO, servedStale = false)
    }

    private fun decode(raw: String, key: String): Envelope? = runCatching {
        objectMapper.readValue(raw, Envelope::class.java)
    }.onFailure {
        log.warn("Discarding invalid Redis cache entry for key={}", key, it)
        runCatching { redis.delete(key) }
    }.getOrNull()

    private fun <T : Any> scheduleRefresh(key: String, retainFor: Duration, loader: () -> T) {
        val lockKey = "$key:refresh-lock"
        val acquired = try {
            redis.opsForValue().setIfAbsent(lockKey, "1", Duration.ofMinutes(2)) == true
        } catch (e: DataAccessException) {
            log.warn("Redis refresh lock failed for key={}; continuing with stale cache", key, e)
            false
        }
        if (!acquired) return

        try {
            refreshExecutor.execute {
                try {
                    write(key, loader(), retainFor)
                } catch (e: Exception) {
                    // Keep the last known-good value. The lock expires so a later request can retry.
                    log.warn("Background refresh failed for public cache key={}; keeping stale value", key, e)
                }
            }
        } catch (e: RuntimeException) {
            log.warn("Could not queue background refresh for public cache key={}", key, e)
        }
    }

    private fun <T : Any> write(key: String, value: T, retainFor: Duration) {
        try {
            val envelope = Envelope(Instant.now(), objectMapper.valueToTree(value))
            redis.opsForValue().set(key, objectMapper.writeValueAsString(envelope), retainFor)
        } catch (e: Exception) {
            log.warn("Redis write failed for public cache key={}; returning loaded value", key, e)
        }
    }
}

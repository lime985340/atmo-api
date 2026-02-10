package lime.atmo.ingestion.kma

import org.slf4j.Logger
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.min
import kotlin.random.Random

/**
 * Minimal retry/backoff for KMA endpoints.
 *
 * - Handles HTTP 429 with Retry-After support (seconds or RFC1123 date).
 * - Retries 5xx as transient as well.
 *
 * Ingestion is a cron-like batch job, so Thread.sleep is acceptable here.
 */
object KmaRetry {
    fun <T> call(
        log: Logger,
        op: String,
        maxAttempts: Int = 6,
        initialDelayMs: Long = 500,
        maxDelayMs: Long = 20_000,
        block: () -> T,
    ): T {
        require(maxAttempts >= 1) { "maxAttempts must be >= 1" }

        var attempt = 1
        var delayMs = initialDelayMs.coerceAtLeast(100)

        while (true) {
            try {
                return block()
            } catch (e: WebClientResponseException) {
                val status = e.statusCode.value()
                val retryable = status == 429 || status in 500..599
                val isLast = attempt >= maxAttempts

                if (!retryable || isLast) {
                    throw e
                }

                val retryAfter = retryAfterMs(e)
                val sleepMs = when {
                    status == 429 && retryAfter != null -> retryAfter
                    else -> jitter(delayMs)
                }.coerceIn(100, maxDelayMs)

                log.warn(
                    "KMA {} failed with HTTP {} (attempt {}/{}). sleep {}ms then retry",
                    op,
                    status,
                    attempt,
                    maxAttempts,
                    sleepMs,
                )

                Thread.sleep(sleepMs)
                attempt++
                delayMs = min(delayMs * 2, maxDelayMs)
            }
        }
    }

    private fun retryAfterMs(e: WebClientResponseException): Long? {
        val raw = e.headers.getFirst("Retry-After")?.trim().orEmpty()
        if (raw.isBlank()) return null

        raw.toLongOrNull()?.let { seconds ->
            return (seconds * 1000L).coerceAtLeast(0)
        }

        return try {
            val whenAt = ZonedDateTime.parse(raw, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant()
            val now = Instant.now()
            (whenAt.toEpochMilli() - now.toEpochMilli()).coerceAtLeast(0)
        } catch (_: Exception) {
            null
        }
    }

    private fun jitter(baseMs: Long): Long {
        // +/- 20% jitter to avoid herding.
        val spread = (baseMs * 0.2).toLong().coerceAtLeast(1)
        return baseMs + Random.nextLong(-spread, spread + 1)
    }
}


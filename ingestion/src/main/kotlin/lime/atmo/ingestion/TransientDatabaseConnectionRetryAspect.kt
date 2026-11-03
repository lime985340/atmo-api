package lime.atmo.ingestion

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.slf4j.LoggerFactory
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

/**
 * Retries an individual Spring Data repository transaction after a transient
 * PostgreSQL connection loss. The retry wraps the repository transaction so
 * each attempt obtains a fresh connection from Hikari.
 */
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 100)
class TransientDatabaseConnectionRetryAspect {
    private val log = LoggerFactory.getLogger(javaClass)

    @Around("this(org.springframework.data.repository.Repository+)")
    fun retryRepositoryCall(joinPoint: ProceedingJoinPoint): Any? {
        var attempt = 1
        while (true) {
            try {
                return joinPoint.proceed()
            } catch (error: Throwable) {
                if (!TransientDatabaseConnectionFailure.matches(error) || attempt >= MAX_ATTEMPTS) {
                    throw error
                }

                val delayMs = RETRY_DELAYS_MS[attempt - 1]
                log.warn(
                    "Transient database connection failure in {} (attempt {}/{}); retrying in {}ms",
                    joinPoint.signature.name,
                    attempt,
                    MAX_ATTEMPTS,
                    delayMs,
                )
                try {
                    Thread.sleep(delayMs)
                } catch (interrupted: InterruptedException) {
                    Thread.currentThread().interrupt()
                    error.addSuppressed(interrupted)
                    throw error
                }
                attempt++
            }
        }
    }

    private companion object {
        const val MAX_ATTEMPTS = 4
        val RETRY_DELAYS_MS = longArrayOf(1_000, 5_000, 15_000)
    }
}

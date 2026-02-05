package lime.atmo.ingestion
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.ExitCodeGenerator
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Profile
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component
import kotlin.system.exitProcess

@Profile("ingestion")
@ConditionalOnProperty(name = ["ingestion.job"])
@Component
class IngestionJobRunner(
    private val runner: IngestionRunner,
    private val context: ApplicationContext,
    private val env: Environment,
) : ApplicationRunner {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun run(args: ApplicationArguments) {
        val job = env.getProperty("INGESTION_JOB")
            ?: env.getProperty("ingestion.job")
            ?: throw IllegalArgumentException("INGESTION_JOB is required (seed|hourly|cleanup|all).")
        val normalized = job.trim().lowercase()

        val exitCode = try {
            when (normalized) {
                "seed" -> runner.seedStationsIfMissing()
                "hourly" -> {
                    runner.ingestAir()
                    runner.ingestWeather()
                    runner.ingestForecast()
                }
                "cleanup" -> runner.cleanupForecasts()
                "all" -> {
                    runner.seedStationsIfMissing()
                    runner.ingestAir()
                    runner.ingestWeather()
                    runner.ingestForecast()
                    runner.cleanupForecasts()
                }
                else -> throw IllegalArgumentException("Unsupported INGESTION_JOB: $job (seed|hourly|cleanup|all)")
            }
            0
        } catch (e: Exception) {
            log.error("Ingestion job failed: {}", normalized, e)
            1
        }

        val code = SpringApplication.exit(context, ExitCodeGenerator { exitCode })
        exitProcess(code)
    }
}

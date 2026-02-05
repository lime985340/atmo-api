package lime.atmo.api.config
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.filter.CorsFilter

@Configuration
class CorsConfig(
    // Comma-separated origins, e.g. "https://app.example.com, http://localhost: 5173"
    // If empty, allow all origins via allowedOriginPatterns (works with allowCredentials = true).
    @Value("\${app.cors.allowed-origins:}")
    private val allowedOriginsCsv: String,
) {

    @Bean
    fun corsFilter(): CorsFilter {
        val cfg = CorsConfiguration()
        cfg.allowCredentials = true

        val origins = allowedOriginsCsv
            .split(", ")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        if (origins.isNotEmpty()) {
            cfg.allowedOrigins = origins
        } else {
            // For local/dev convenience; restrict in production by setting app.cors.allowed-origins
            cfg.allowedOriginPatterns = listOf("*")
        }

        cfg.allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        cfg.allowedHeaders = listOf("*")

        val src = UrlBasedCorsConfigurationSource()
        src.registerCorsConfiguration("/**", cfg)
        return CorsFilter(src)
    }
}

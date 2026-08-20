package lime.atmo.api.config
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.filter.CorsFilter

@Configuration
class CorsConfig(
    // Comma-separated origins, e.g. "https://app.example.com, http://localhost:5173"
    // If empty, use the local/Quick Tunnel development patterns below.
    @Value("\${app.cors.allowed-origins:}")
    private val allowedOriginsCsv: String,
) {

    @Bean
    fun corsFilter(): CorsFilter {
        val cfg = CorsConfiguration()
        cfg.allowCredentials = true

        val origins = allowedOriginsCsv
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        if (origins.isNotEmpty()) {
            cfg.allowedOrigins = origins
        } else {
            // Quick Tunnel hostnames change whenever the tunnel pod is recreated.
            // Use explicit APP_CORS_ALLOWED_ORIGINS for a fixed production domain.
            cfg.allowedOriginPatterns = listOf(
                "http://localhost:*",
                "http://127.0.0.1:*",
                "https://*.trycloudflare.com",
            )
        }

        cfg.allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        cfg.allowedHeaders = listOf("*")

        val src = UrlBasedCorsConfigurationSource()
        src.registerCorsConfiguration("/**", cfg)
        return CorsFilter(src)
    }
}

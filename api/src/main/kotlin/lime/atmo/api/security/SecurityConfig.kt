package lime.atmo.api.security

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableConfigurationProperties(JwtProps::class)
class SecurityConfig(private val jwtService: JwtService) {

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http.csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests {
                it.requestMatchers(
                    "/actuator/health",
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/api/v1/dashboard",
                    "/api/v1/auth/**",
                    "/api/v1/stations/**",
                    "/api/v1/map/**",
                ).permitAll()
                it.anyRequest().authenticated()
            }
            .addFilterBefore(
                JwtAuthenticationFilter(jwtService),
                UsernamePasswordAuthenticationFilter::class.java,
            )

        return http.build()
    }
}

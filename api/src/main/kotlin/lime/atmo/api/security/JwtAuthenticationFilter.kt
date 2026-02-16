package lime.atmo.api.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter

class JwtAuthenticationFilter(
    private val jwtService: JwtService,
) : OncePerRequestFilter() {
    private val log = LoggerFactory.getLogger(JwtAuthenticationFilter::class.java)

    override fun doFilterInternal(
        req: HttpServletRequest,
        res: HttpServletResponse,
        chain: FilterChain,
    ) {
        val h = req.getHeader("Authorization") ?: ""
        val uri = req.requestURI

        if (!h.startsWith("Bearer ")) {
            if (uri.contains("/favorites") || uri.contains("/me")) {
                log.info("No Bearer token for protected endpoint: {}, Authorization header: {}", uri, h.take(20))
            }
            chain.doFilter(req, res)
            return
        }

        val token = h.removePrefix("Bearer ").trim()
        val userId = jwtService.parseAccessUserId(token)

        if (userId != null) {
            SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken(
                userId,
                null,
                listOf(SimpleGrantedAuthority("ROLE_USER")),
            )
            log.debug("Authenticated user: {} for URI: {}", userId, uri)
        } else {
            log.warn("Invalid or expired access token for URI: {}, token prefix: {}", uri, token.take(10))
        }
        chain.doFilter(req, res)
    }
}

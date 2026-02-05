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
        if (!h.startsWith("Bearer ")) {
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
        } else {
            log.debug("Invalid or expired access token provided")
        }
        chain.doFilter(req, res)
    }
}

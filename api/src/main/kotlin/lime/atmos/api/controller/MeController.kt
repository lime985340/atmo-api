package lime.atmos.api.controller

import lime.atmos.application.port.UserPort
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1")
class MeController(
    private val users: UserPort
) {
    @GetMapping("/me")
    fun me(auth: Authentication?): Any {
        val userId = auth?.principal as? UUID ?: return mapOf("authenticated" to false)
        val u = users.findById(userId) ?: return mapOf("authenticated" to false)

        return mapOf(
            "authenticated" to true,
            "id" to u.id,
            "provider" to u.provider,
            "email" to u.email,
            "name" to u.name
        )
    }
}

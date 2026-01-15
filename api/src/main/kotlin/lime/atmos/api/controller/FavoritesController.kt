package lime.atmos.api.controller

import lime.atmos.application.port.FavoritePort
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import java.util.UUID

data class FavoriteRequest(val locationId: Long)

@RestController
@RequestMapping("/api/v1/favorites")
class FavoritesController(
    private val favorites: FavoritePort
) {
    private fun uid(auth: Authentication?): UUID =
        (auth?.principal as? UUID) ?: throw IllegalArgumentException("unauthorized")

    @GetMapping
    fun list(auth: Authentication?) = favorites.listFavorites(uid(auth))

    @PostMapping
    fun add(auth: Authentication?, @RequestBody req: FavoriteRequest): ResponseEntity<Any> {
        favorites.addFavorite(uid(auth), req.locationId)
        return ResponseEntity.ok(mapOf("added" to req.locationId))
    }

    @DeleteMapping("/{locationId}")
    fun remove(auth: Authentication?, @PathVariable locationId: Long): ResponseEntity<Any> {
        favorites.removeFavorite(uid(auth), locationId)
        return ResponseEntity.ok(mapOf("removed" to locationId))
    }
}

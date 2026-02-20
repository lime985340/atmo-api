package lime.atmo.api.controller

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import lime.atmo.api.exception.UnauthorizedException
import lime.atmo.application.port.CreateFavoriteRequest
import lime.atmo.application.port.FavoriteDashboardCard
import lime.atmo.application.port.FavoriteDashboardPort
import lime.atmo.application.port.FavoritePort
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Duration
import java.util.UUID

@RestController
@RequestMapping("/api/v1/favorites")
class FavoritesController(
    private val favorites: FavoritePort,
    private val dashboardPort: FavoriteDashboardPort,
    private val redis: StringRedisTemplate,
    private val objectMapper: ObjectMapper,
) {
    private fun uid(auth: Authentication?): UUID =
        (auth?.principal as? UUID) ?: throw UnauthorizedException("unauthorized")

    @GetMapping
    fun list(auth: Authentication?) = favorites.listFavorites(uid(auth))

    @PostMapping
    fun create(auth: Authentication?, @RequestBody req: CreateFavoriteRequest): ResponseEntity<Any> {
        val created = favorites.createFavorite(uid(auth), req.description, req.lat, req.lng, req.address)
        // invalidate dashboard cache
        redis.delete("favorites:dashboard:${uid(auth)}")
        return ResponseEntity.ok(created)
    }

    @DeleteMapping("/{favoriteId}")
    fun remove(auth: Authentication?, @PathVariable favoriteId: Long): ResponseEntity<Any> {
        val userId = uid(auth)
        favorites.removeFavorite(userId, favoriteId)
        redis.delete("favorites:dashboard:$userId")
        return ResponseEntity.ok(mapOf("removed" to favoriteId))
    }

    @GetMapping("/dashboard")
    fun dashboard(auth: Authentication?): List<FavoriteDashboardCard> {
        val userId = uid(auth)
        val key = "favorites:dashboard:$userId"
        // Try cache first
        val cached = redis.opsForValue().get(key)
        if (cached != null) {
            return objectMapper.readValue(cached, object : TypeReference<List<FavoriteDashboardCard>>() {})
        }

        // Fetch from use case
        val cards = dashboardPort.getDashboard(userId)

        // Cache result (30 seconds)
        redis.opsForValue().set(key, objectMapper.writeValueAsString(cards), Duration.ofSeconds(30))
        return cards
    }
}

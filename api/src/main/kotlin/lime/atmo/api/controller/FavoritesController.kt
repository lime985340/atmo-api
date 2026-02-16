package lime.atmo.api.controller

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import lime.atmo.application.port.FavoritePort
import lime.atmo.application.port.WeatherReadPort
import lime.atmo.domain.model.WeatherHour
import lime.atmo.infra.jpa.repo.AirCurrentRepo
import lime.atmo.infra.jpa.repo.StationWeatherCurrentRepo
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
import java.time.Instant
import java.util.UUID

data class CreateFavoriteRequest(val description: String, val lat: Double, val lng: Double, val address: String? = null)

@RestController
@RequestMapping("/api/v1/favorites")
class FavoritesController(
    private val favorites: FavoritePort,
    private val airCurrentRepo: AirCurrentRepo,
    private val stationWeatherCurrentRepo: StationWeatherCurrentRepo,
    private val weatherReadPort: WeatherReadPort,
    private val redis: StringRedisTemplate,
    private val objectMapper: ObjectMapper,
) {
    private fun uid(auth: Authentication?): UUID =
        (auth?.principal as? UUID) ?: throw IllegalArgumentException("unauthorized")

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

    data class FavoriteDashboardCard(
        val id: Long,
        val description: String,
        val lat: Double,
        val lng: Double,
        val address: String?,
        val weatherObsTime: String?,
        val tempC: Float?,
        val humidity: Float?,
        val windSpeed: Float?,
        val precipType: String?,
        val precipMm: Float?,
        val airDataTime: Instant?,
        val pm10: Float?,
        val pm25: Float?,
        val grade: Int?,
        val forecastHours: List<FavoriteForecastHour>,
        val stale: Boolean,
    )

    data class FavoriteForecastHour(
        val forecastAt: Instant,
        val tempC: Double?,
        val humidity: Double?,
        val windSpeed: Double?,
        val pop: Double?,
        val sky: String?,
        val precipType: String?,
        val precipMm: Double?,
    )

    @GetMapping("/dashboard")
    fun dashboard(auth: Authentication?): List<FavoriteDashboardCard> {
        val userId = uid(auth)
        val key = "favorites:dashboard:$userId"
        val cached = redis.opsForValue().get(key)
        if (cached != null) {
            return objectMapper.readValue(cached, object : TypeReference<List<FavoriteDashboardCard>>() {})
        }

        val favs = favorites.listFavorites(userId)
        val now = Instant.now()
        val forecastUntil = now.plus(Duration.ofHours(6))

        val cards = favs.map { f ->
            // 실시간으로 가장 가까운 측정소 찾기
            val nearestStationId = favorites.findNearestStationId(f.lat, f.lng)

            val air = nearestStationId?.let { airCurrentRepo.findByStationId(it) }
            val w = nearestStationId?.let { stationWeatherCurrentRepo.findByStationId(it) }
            val forecast = nearestStationId?.let { weatherReadPort.getNextHours(it, now, forecastUntil) } ?: emptyList()

            val updatedAt = listOfNotNull(air?.dataTime, w?.updatedAt).maxOrNull()
            val stale = updatedAt?.let { Duration.between(it, now) > Duration.ofMinutes(30) } ?: true

            FavoriteDashboardCard(
                id = f.id,
                description = f.description,
                lat = f.lat,
                lng = f.lng,
                address = f.address,
                weatherObsTime = w?.obsTime,
                tempC = w?.temp?.toFloat(),
                humidity = w?.humidity?.toFloat(),
                windSpeed = w?.windSpeed?.toFloat(),
                precipType = w?.precip?.let { if (it > 0.0) "RAIN" else "NONE" },
                precipMm = w?.precip?.toFloat(),
                airDataTime = air?.dataTime,
                pm10 = air?.pm10,
                pm25 = air?.pm25,
                grade = air?.grade,
                forecastHours = forecast.map { it.toCardDto() },
                stale = stale,
            )
        }

        redis.opsForValue().set(key, objectMapper.writeValueAsString(cards), Duration.ofSeconds(30))
        return cards
    }
}

private fun WeatherHour.toCardDto() = FavoritesController.FavoriteForecastHour(
    forecastAt = forecastAt,
    tempC = tempC,
    humidity = humidity,
    windSpeed = windSpeed,
    pop = pop,
    sky = sky,
    precipType = precipType,
    precipMm = precipMm,
)

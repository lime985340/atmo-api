package lime.atmo.api.controller

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import lime.atmo.infra.jpa.repo.AirCurrentRepo
import lime.atmo.infra.jpa.repo.StationRepo
import lime.atmo.infra.jpa.repo.StationWeatherCurrentRepo
import org.slf4j.LoggerFactory
import org.springframework.data.redis.RedisConnectionFailureException
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Duration
import java.time.Instant

@RestController
@RequestMapping("/api/v1/map")
class MapController(
    private val stationRepo: StationRepo,
    private val airCurrentRepo: AirCurrentRepo,
    private val stationWeatherCurrentRepo: StationWeatherCurrentRepo,
    private val redis: StringRedisTemplate,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    data class AirMarkerDto(
        val stationId: Long,
        val stationName: String,
        val sido: String?,
        val addr: String?,
        val lat: Double,
        val lng: Double,
        val dataTime: Instant?,
        val pm10: Float?,
        val pm25: Float?,
        val grade: Int?,
        val stale: Boolean,
    )

    data class WeatherMarkerDto(
        val stationId: Long,
        val stationName: String,
        val lat: Double,
        val lng: Double,
        val obsTime: String?,
        val temp: Double?,
        val humidity: Double?,
        val windSpeed: Double?,
        val precip: Double?,
        val stale: Boolean,
    )

    data class Bbox(val minLat: Double, val minLng: Double, val maxLat: Double, val maxLng: Double) {
        companion object {
            fun parse(raw: String): Bbox {
                // raw: "minLat,minLng,maxLat,maxLng"
                val p = raw.split(",").map { it.trim() }
                require(p.size == 4) { "bbox must be 'minLat,minLng,maxLat,maxLng'" }
                return Bbox(p[0].toDouble(), p[1].toDouble(), p[2].toDouble(), p[3].toDouble())
            }
        }
    }

    private fun normBbox(raw: String): String {
        // Normalize to reduce cache misses from tiny float differences.
        return raw.split(",").map { it.trim().toDouble() }.joinToString(",") { String.format(java.util.Locale.US, "%.4f", it) }
    }

    private fun <T> getOrSetJson(key: String, ttlSeconds: Long, typeRef: TypeReference<T>, supplier: () -> T): T {
        val cached = try {
            redis.opsForValue().get(key)
        } catch (e: RedisConnectionFailureException) {
            log.warn("Redis read failed for key={}, bypassing cache", key, e)
            null
        }
        if (cached != null) {
            return objectMapper.readValue(cached, typeRef)
        }
        val value = supplier()
        try {
            redis.opsForValue().set(key, objectMapper.writeValueAsString(value), Duration.ofSeconds(ttlSeconds))
        } catch (e: RedisConnectionFailureException) {
            log.warn("Redis write failed for key={}, returning uncached response", key, e)
        }
        return value
    }

    @GetMapping("/air-markers")
    fun getAirMarkers(
        @RequestParam bbox: String,
        @RequestParam(required = false, defaultValue = "8") level: Int,
        @RequestParam(required = false, defaultValue = "20") cacheTtlSeconds: Long,
        @RequestParam(required = false, defaultValue = "3600") staleSeconds: Long,
    ): List<AirMarkerDto> {
        val key = "map:air:$level:${normBbox(bbox)}"
        val typeRef = object : TypeReference<List<AirMarkerDto>>() {}
        return getOrSetJson(key, cacheTtlSeconds, typeRef) {
            val b = Bbox.parse(bbox)
            val staleCutoff = Instant.now().minusSeconds(staleSeconds)

            val stations = stationRepo.findActiveInBbox(b.minLat, b.maxLat, b.minLng, b.maxLng)
            if (stations.isEmpty()) {
                emptyList()
            } else {
                val currentById = airCurrentRepo.findAllById(stations.mapNotNull { it.id }).associateBy { it.stationId }

                val markers = stations.mapNotNull { s ->
                    val id = s.id ?: return@mapNotNull null
                    val c = currentById[id]
                    val dataTime = c?.dataTime
                    AirMarkerDto(
                        stationId = id,
                        stationName = s.stationName,
                        sido = s.sido,
                        addr = s.addr,
                        lat = s.lat ?: return@mapNotNull null,
                        lng = s.lng ?: return@mapNotNull null,
                        dataTime = dataTime,
                        pm10 = c?.pm10,
                        pm25 = c?.pm25,
                        grade = c?.grade,
                        stale = dataTime == null || dataTime.isBefore(staleCutoff),
                    )
                }
                markers
            }
        }
    }

    @GetMapping("/weather-markers")
    fun getWeatherMarkers(
        @RequestParam bbox: String,
        @RequestParam(required = false, defaultValue = "8") level: Int,
        @RequestParam(required = false, defaultValue = "20") cacheTtlSeconds: Long,
        @RequestParam(required = false, defaultValue = "3600") staleSeconds: Long,
    ): List<WeatherMarkerDto> {
        val key = "map:weather:$level:${normBbox(bbox)}"
        val typeRef = object : TypeReference<List<WeatherMarkerDto>>() {}
        return getOrSetJson(key, cacheTtlSeconds, typeRef) {
            val b = Bbox.parse(bbox)
            val staleCutoff = Instant.now().minusSeconds(staleSeconds)

            val stations = stationRepo.findActiveInBbox(b.minLat, b.maxLat, b.minLng, b.maxLng)
            if (stations.isEmpty()) {
                emptyList()
            } else {
                val currentById = stationWeatherCurrentRepo.findAllByStationIdIn(stations.mapNotNull { it.id }).associateBy { it.stationId }
                val markers = stations.mapNotNull { st ->
                    val id = st.id ?: return@mapNotNull null
                    val c = currentById[id]
                    val obsTime = c?.obsTime
                    val updatedAt = c?.updatedAt
                    WeatherMarkerDto(
                        stationId = id,
                        stationName = st.stationName,
                        lat = st.lat ?: return@mapNotNull null,
                        lng = st.lng ?: return@mapNotNull null,
                        obsTime = obsTime,
                        temp = c?.temp,
                        humidity = c?.humidity,
                        windSpeed = c?.windSpeed,
                        precip = c?.precip,
                        stale = updatedAt == null || updatedAt.isBefore(staleCutoff),
                    )
                }
                markers
            }
        }
    }
}

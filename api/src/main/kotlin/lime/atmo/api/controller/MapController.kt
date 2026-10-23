package lime.atmo.api.controller

import lime.atmo.api.station.PublicStationSnapshotService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
@RequestMapping("/api/v1/map")
class MapController(
    private val stationSnapshot: PublicStationSnapshotService,
) {
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

    private data class Bbox(val minLat: Double, val minLng: Double, val maxLat: Double, val maxLng: Double) {
        fun contains(lat: Double, lng: Double): Boolean = lat in minLat..maxLat && lng in minLng..maxLng

        companion object {
            fun parse(raw: String): Bbox {
                val parts = raw.split(",").map { it.trim() }
                require(parts.size == 4) { "bbox must be 'minLat,minLng,maxLat,maxLng'" }
                return Bbox(parts[0].toDouble(), parts[1].toDouble(), parts[2].toDouble(), parts[3].toDouble())
            }
        }
    }

    @GetMapping("/air-markers")
    @Suppress("UNUSED_PARAMETER")
    fun getAirMarkers(
        @RequestParam bbox: String,
        @RequestParam(required = false, defaultValue = "8") level: Int,
        @RequestParam(required = false, defaultValue = "3600") staleSeconds: Long,
    ): List<AirMarkerDto> {
        val bounds = Bbox.parse(bbox)
        val staleCutoff = Instant.now().minusSeconds(staleSeconds.coerceAtLeast(0))
        val snapshot = stationSnapshot.get()
        return snapshot.value.stations.mapNotNull { station ->
            val lat = station.lat ?: return@mapNotNull null
            val lng = station.lng ?: return@mapNotNull null
            if (!bounds.contains(lat, lng)) return@mapNotNull null
            val air = station.air
            val dataTime = air?.dataTime
            AirMarkerDto(
                stationId = station.id,
                stationName = station.name,
                sido = station.sido,
                addr = station.addr,
                lat = lat,
                lng = lng,
                dataTime = dataTime,
                pm10 = air?.pm10,
                pm25 = air?.pm25,
                grade = air?.grade,
                stale = snapshot.servedStale || dataTime == null || dataTime.isBefore(staleCutoff),
            )
        }
    }

    @GetMapping("/weather-markers")
    @Suppress("UNUSED_PARAMETER")
    fun getWeatherMarkers(
        @RequestParam bbox: String,
        @RequestParam(required = false, defaultValue = "8") level: Int,
        @RequestParam(required = false, defaultValue = "3600") staleSeconds: Long,
    ): List<WeatherMarkerDto> {
        val bounds = Bbox.parse(bbox)
        val staleCutoff = Instant.now().minusSeconds(staleSeconds.coerceAtLeast(0))
        val snapshot = stationSnapshot.get()
        return snapshot.value.stations.mapNotNull { station ->
            val lat = station.lat ?: return@mapNotNull null
            val lng = station.lng ?: return@mapNotNull null
            if (!bounds.contains(lat, lng)) return@mapNotNull null
            val weather = station.weather
            val updatedAt = weather?.updatedAt
            WeatherMarkerDto(
                stationId = station.id,
                stationName = station.name,
                lat = lat,
                lng = lng,
                obsTime = weather?.obsTime,
                temp = weather?.temp,
                humidity = weather?.humidity,
                windSpeed = weather?.windSpeed,
                precip = weather?.precip,
                stale = snapshot.servedStale || updatedAt == null || updatedAt.isBefore(staleCutoff),
            )
        }
    }
}

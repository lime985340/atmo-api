package lime.atmo.api.station

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import lime.atmo.api.exception.NotFoundException
import lime.atmo.application.port.WeatherReadPort
import lime.atmo.domain.model.WeatherHour
import lime.atmo.infra.jpa.repo.AirCurrentRepo
import lime.atmo.infra.jpa.repo.StationRepo
import lime.atmo.infra.jpa.repo.StationWeatherCurrentRepo
import org.slf4j.LoggerFactory
import org.springframework.data.redis.RedisConnectionFailureException
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Duration
import java.time.Instant
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

/**
 * 측정소 및 측정소별 데이터 조회 API
 *
 * 카카오 지도 연동을 위한 측정소 위치 및 실시간 데이터 제공
 */
@RestController
@RequestMapping("/api/v1/stations")
class StationController(
    private val stationRepo: StationRepo,
    private val airCurrentRepo: AirCurrentRepo,
    private val stationWeatherCurrentRepo: StationWeatherCurrentRepo,
    private val weatherReadPort: WeatherReadPort,
    private val redis: StringRedisTemplate,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private fun normCoord(value: Double): String = String.format(Locale.US, "%.4f", value)

    private fun bboxCacheKey(minLat: Double, minLng: Double, maxLat: Double, maxLng: Double, limit: Int): String =
        "stations:bbox:${normCoord(minLat)},${normCoord(minLng)},${normCoord(maxLat)},${normCoord(maxLng)}:limit=$limit"

    private fun readCachedStations(key: String): BboxStationsResponse? {
        val raw = try {
            redis.opsForValue().get(key)
        } catch (e: RedisConnectionFailureException) {
            log.warn("Redis read failed for key={}, bypassing cache", key, e)
            null
        }
        return raw?.let { objectMapper.readValue(it, object : TypeReference<BboxStationsResponse>() {}) }
    }

    private fun writeCachedStations(key: String, value: BboxStationsResponse) {
        try {
            redis.opsForValue().set(key, objectMapper.writeValueAsString(value), Duration.ofSeconds(20))
        } catch (e: RedisConnectionFailureException) {
            log.warn("Redis write failed for key={}, returning uncached response", key, e)
        }
    }

    /**
     * 모든 활성 측정소 목록 조회 (대기질 + 날씨 데이터 포함)
     *
     * GET /api/v1/stations
     *
     * 응답 예시:
     * {
     *   "stations": [
     *     {
     *       "id": 1,
     *       "name": "종로구",
     *       "addr": "서울 종로구 효제동 173-2",
     *       "sido": "서울",
     *       "lat": 37.572025,
     *       "lng": 127.005028,
     *       "air": {
     *         "pm10": 45.0,
     *         "pm25": 23.0,
     *         "grade": 2,
     *         "dataTime": "2026-02-02T14:00:00Z"
     *       },
     *       "weather": {
     *         "temp": 5.2,
     *         "humidity": 65.0,
     *         "wind": 2.3,
     *         "rain": 0.0,
     *         "obsTime": "20260202 1400"
     *       }
     *     }
     *   ]
     * }
     */
    @GetMapping
    @Transactional(readOnly = true)
    fun getAllStations(
        @RequestParam(required = false) sido: String?,
    ): StationsResponse {
        val stations = if (sido != null) {
            stationRepo.findAllByIsActiveTrueAndSido(sido)
        } else {
            stationRepo.findAllByIsActiveTrue()
        }

        val stationIds = stations.mapNotNull { it.id }
        val airByStationId = airCurrentRepo.findAllByStationIdIn(stationIds).associateBy { it.stationId }
        val weatherByStationId = stationWeatherCurrentRepo.findAllByStationIdIn(stationIds).associateBy { it.stationId }

        val stationDtos = stations.mapNotNull { station ->
            val stationId = station.id ?: return@mapNotNull null

            // 대기질·날씨는 측정소별 개별 조회 대신 배치 조회 결과를 사용합니다.
            val air = airByStationId[stationId]?.let {
                AirDataDto(
                    pm10 = it.pm10,
                    pm25 = it.pm25,
                    grade = it.grade,
                    dataTime = it.dataTime,
                )
            }

            // 날씨 데이터 조회
            val weather = weatherByStationId[stationId]?.let {
                WeatherDataDto(
                    temp = it.temp,
                    humidity = it.humidity,
                    windSpeed = it.windSpeed,
                    precip = it.precip,
                    obsTime = it.obsTime,
                )
            }

            StationDto(
                id = stationId,
                name = station.stationName,
                addr = station.addr,
                sido = station.sido,
                regionCode = station.regionCode,
                lat = station.lat,
                lng = station.lng,
                air = air,
                weather = weather,
            )
        }

        return StationsResponse(stations = stationDtos)
    }

    /**
     * 특정 측정소 상세 조회
     *
     * GET /api/v1/stations/{id}
     */
    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    fun getStation(@PathVariable id: Long): StationDto {
        val station = stationRepo.findById(id).orElseThrow {
            NotFoundException("Station not found: $id")
        }

        val air = station.id?.let { stationId ->
            airCurrentRepo.findByStationId(stationId)?.let {
                AirDataDto(
                    pm10 = it.pm10,
                    pm25 = it.pm25,
                    grade = it.grade,
                    dataTime = it.dataTime,
                )
            }
        }

        val weather = station.id?.let { stationId ->
            stationWeatherCurrentRepo.findByStationId(stationId)?.let {
                WeatherDataDto(
                    temp = it.temp,
                    humidity = it.humidity,
                    windSpeed = it.windSpeed,
                    precip = it.precip,
                    obsTime = it.obsTime,
                )
            }
        }

        return StationDto(
            id = station.id!!,
            name = station.stationName,
            addr = station.addr,
            sido = station.sido,
            regionCode = station.regionCode,
            lat = station.lat,
            lng = station.lng,
            air = air,
            weather = weather,
        )
    }

    /**
     * 특정 측정소 시간대 예보 조회
     *
     * GET /api/v1/stations/{id}/forecast?hours=6
     */
    @GetMapping("/{id}/forecast")
    @Transactional(readOnly = true)
    fun getForecast(
        @PathVariable id: Long,
        @RequestParam(required = false, defaultValue = "6") hours: Long,
    ): List<WeatherForecastDto> {
        val stationId = stationRepo.findById(id).orElseThrow {
            NotFoundException("Station not found: $id")
        }.id ?: throw NotFoundException("Station not found: $id")

        val now = Instant.now()
        val until = now.plus(Duration.ofHours(hours.coerceIn(1, 24)))
        return weatherReadPort.getNextHours(stationId, now, until).map { it.toDto() }
    }

    /**
     * 시도별 측정소 그룹화 조회
     *
     * GET /api/v1/stations/by-sido
     *
     * 응답: { "서울": [...], "부산": [...], ... }
     */
    @GetMapping("/by-sido")
    @Transactional(readOnly = true)
    fun getStationsBySido(): Map<String, List<StationDto>> {
        val allStations = getAllStations(sido = null)
        return allStations.stations.groupBy { it.sido ?: "기타" }
    }

    /**
     * 지도 바운드 기준 측정소 조회
     *
     * GET /api/v1/stations/bbox?swLat&swLng&neLat&neLng&limit
     */
    @GetMapping("/bbox")
    @Transactional(readOnly = true)
    fun getStationsInBbox(
        @RequestParam swLat: Double,
        @RequestParam swLng: Double,
        @RequestParam neLat: Double,
        @RequestParam neLng: Double,
        @RequestParam(required = false, defaultValue = "500") limit: Int,
    ): BboxStationsResponse {
        val minLat = min(swLat, neLat)
        val maxLat = max(swLat, neLat)
        val minLng = min(swLng, neLng)
        val maxLng = max(swLng, neLng)
        val safeLimit = limit.coerceAtLeast(1)
        val cacheKey = bboxCacheKey(minLat, minLng, maxLat, maxLng, safeLimit)
        val cached = readCachedStations(cacheKey)

        return try {
            val stations = stationRepo.findActiveInBbox(minLat, maxLat, minLng, maxLng)
            val stationDtos = stations.mapNotNull { station ->
                val stationId = station.id ?: return@mapNotNull null
                val lat = station.lat ?: return@mapNotNull null
                val lng = station.lng ?: return@mapNotNull null
                BboxStationDto(
                    id = stationId,
                    name = station.stationName,
                    addr = station.addr,
                    sido = station.sido,
                    regionCode = station.regionCode,
                    lat = lat,
                    lng = lng,
                )
            }
            val trimmed = stationDtos.take(safeLimit)
            BboxStationsResponse(stations = trimmed).also { writeCachedStations(cacheKey, it) }
        } catch (e: Exception) {
            if (cached != null) {
                log.warn("BBox DB read failed; serving cached response for key={}", cacheKey, e)
                cached
            } else {
                throw e
            }
        }
    }
}

// DTO 클래스들
data class StationsResponse(
    val stations: List<StationDto>,
)

data class BboxStationsResponse(
    val stations: List<BboxStationDto>,
)

data class StationDto(
    val id: Long,
    val name: String,
    val addr: String?,
    val sido: String?,
    val regionCode: String?,
    val lat: Double?,
    val lng: Double?,
    val air: AirDataDto?,
    val weather: WeatherDataDto?,
)

data class BboxStationDto(
    val id: Long,
    val name: String,
    val addr: String?,
    val sido: String?,
    val regionCode: String?,
    val lat: Double,
    val lng: Double,
)

data class AirDataDto(
    val pm10: Float?,
    val pm25: Float?,
    val grade: Int?,
    val dataTime: java.time.Instant?,
)

data class WeatherDataDto(
    val temp: Double?,
    val humidity: Double?,
    val windSpeed: Double?,
    val precip: Double?,
    val obsTime: String?,
)

data class WeatherForecastDto(
    val forecastAt: Instant,
    val tempC: Double?,
    val humidity: Double?,
    val windSpeed: Double?,
    val pop: Double?,
    val sky: String?,
    val precipType: String?,
    val precipMm: Double?,
)

private fun WeatherHour.toDto() = WeatherForecastDto(
    forecastAt = forecastAt,
    tempC = tempC,
    humidity = humidity,
    windSpeed = windSpeed,
    pop = pop,
    sky = sky,
    precipType = precipType,
    precipMm = precipMm,
)

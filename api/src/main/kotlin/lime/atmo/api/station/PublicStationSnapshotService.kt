package lime.atmo.api.station

import com.fasterxml.jackson.core.type.TypeReference
import lime.atmo.api.cache.StaleRedisReadCache
import lime.atmo.infra.jpa.repo.AirCurrentRepo
import lime.atmo.infra.jpa.repo.StationRepo
import lime.atmo.infra.jpa.repo.StationWeatherCurrentRepo
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.Duration

/**
 * Loads the public station and current-condition dataset once, then shares the same bounded
 * snapshot between station and map endpoints. This avoids a separate Neon query for each map box.
 */
@Service
class PublicStationSnapshotService(
    private val stationRepo: StationRepo,
    private val airCurrentRepo: AirCurrentRepo,
    private val stationWeatherCurrentRepo: StationWeatherCurrentRepo,
    private val cache: StaleRedisReadCache,
    @Value("\${atmo.cache.public.fresh-seconds:21600}") private val freshSeconds: Long,
    @Value("\${atmo.cache.public.retain-seconds:1209600}") private val retainSeconds: Long,
) {
    fun get(): StaleRedisReadCache.Read<StationsResponse> = cache.getOrLoad(
        key = CACHE_KEY,
        type = object : TypeReference<StationsResponse>() {},
        freshFor = Duration.ofSeconds(freshSeconds),
        retainFor = Duration.ofSeconds(retainSeconds),
        loader = ::loadSnapshot,
    )

    private fun loadSnapshot(): StationsResponse {
        val stations = stationRepo.findAllByIsActiveTrue()
        val stationIds = stations.mapNotNull { it.id }
        val airByStationId = airCurrentRepo.findAllByStationIdIn(stationIds).associateBy { it.stationId }
        val weatherByStationId = stationWeatherCurrentRepo.findAllByStationIdIn(stationIds).associateBy { it.stationId }

        val stationDtos = stations.mapNotNull { station ->
            val stationId = station.id ?: return@mapNotNull null
            val air = airByStationId[stationId]?.let {
                AirDataDto(pm10 = it.pm10, pm25 = it.pm25, grade = it.grade, dataTime = it.dataTime)
            }
            val weather = weatherByStationId[stationId]?.let {
                WeatherDataDto(
                    temp = it.temp,
                    humidity = it.humidity,
                    windSpeed = it.windSpeed,
                    precip = it.precip,
                    obsTime = it.obsTime,
                    updatedAt = it.updatedAt,
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

    companion object {
        const val CACHE_KEY = "atmo:public:stations:v1"
    }
}

package lime.atmo.infra.adapter

import lime.atmo.application.port.FavoriteDashboardCard
import lime.atmo.application.port.FavoriteDashboardPort
import lime.atmo.application.port.FavoriteForecastHour
import lime.atmo.application.port.FavoritePort
import lime.atmo.application.port.WeatherReadPort
import lime.atmo.domain.model.WeatherHour
import lime.atmo.infra.jpa.repo.AirCurrentRepo
import lime.atmo.infra.jpa.repo.StationWeatherCurrentRepo
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID

@Component
class FavoriteDashboardAdapter(
    private val favoritePort: FavoritePort,
    private val airCurrentRepo: AirCurrentRepo,
    private val stationWeatherCurrentRepo: StationWeatherCurrentRepo,
    private val weatherReadPort: WeatherReadPort,
) : FavoriteDashboardPort {

    override fun getDashboard(userId: UUID): List<FavoriteDashboardCard> {
        val favs = favoritePort.listFavorites(userId)
        val now = Instant.now()
        val forecastUntil = now.plusSeconds(6 * 60 * 60)

        return favs.map { f ->
            val nearestStationId = favoritePort.findNearestStationId(f.lat, f.lng)

            val air = nearestStationId?.let { airCurrentRepo.findByStationId(it) }
            val w = nearestStationId?.let { stationWeatherCurrentRepo.findByStationId(it) }
            val forecast = nearestStationId?.let { weatherReadPort.getNextHours(it, now, forecastUntil) } ?: emptyList()

            val updatedAt = listOfNotNull(air?.dataTime, w?.updatedAt).maxOrNull()
            val stale = updatedAt?.let { java.time.Duration.between(it, now).toMinutes() > 30 } ?: true

            FavoriteDashboardCard(
                id = f.id,
                stationId = nearestStationId,
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
    }

    private fun WeatherHour.toCardDto() = FavoriteForecastHour(
        forecastAt = forecastAt,
        tempC = tempC,
        humidity = humidity,
        windSpeed = windSpeed,
        pop = pop,
        sky = sky,
        precipType = precipType,
        precipMm = precipMm,
    )
}

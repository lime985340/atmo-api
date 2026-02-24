package lime.atmo.application.port

import lime.atmo.domain.model.WeatherHour
import lime.atmo.domain.model.WeatherNow
import java.time.Instant

interface WeatherReadPort {
    fun getWeatherNow(stationId: Long): WeatherNow?
    fun getNextHours(stationId: Long, from: Instant, to: Instant): List<WeatherHour>
    fun getLatestUpdateTime(stationId: Long): Instant?
}

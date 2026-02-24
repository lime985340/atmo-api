package lime.atmo.application.port

import lime.atmo.domain.model.AirNow
import java.time.Instant

interface AirReadPort {
    fun getAirNow(stationId: Long): AirNow?
    fun getLatestUpdateTime(stationId: Long): Instant?
}

package lime.atmo.infra.adapter

import lime.atmo.application.port.AirReadPort
import lime.atmo.domain.model.AirNow
import lime.atmo.infra.jpa.repo.AirCurrentRepo
import org.springframework.stereotype.Component
import java.time.Instant

/**
 * 대기오염 관련 Port 구현체
 * - 현재 대기오염 정보 조회
 */
@Component
class AirReadAdapter(
    private val repo: AirCurrentRepo,
) : AirReadPort {

    override fun getAirNow(stationId: Long): AirNow? {
        return repo.findByStationId(stationId)?.let {
            AirNow(
                measuredAt = it.dataTime,
                khai = null,
                pm10 = it.pm10?.toDouble(),
                pm25 = it.pm25?.toDouble(),
                o3 = null,
                grade = it.grade,
            )
        }
    }

    override fun getLatestUpdateTime(stationId: Long): Instant? {
        return repo.findByStationId(stationId)?.dataTime
    }
}

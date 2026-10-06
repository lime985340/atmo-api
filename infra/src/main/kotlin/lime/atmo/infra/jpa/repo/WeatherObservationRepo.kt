package lime.atmo.infra.jpa.repo

import lime.atmo.infra.jpa.entity.WeatherObservationEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

interface WeatherObservationRepo : JpaRepository<WeatherObservationEntity, Long> {
    fun findByStationIdAndObservedAt(stationId: Long, observedAt: Instant): WeatherObservationEntity?

    @Modifying
    @Transactional
    @Query(
        value = """
            insert into weather_observations (
                station_id, observed_at, temp, humidity, wind_speed, precip, created_at
            ) values (
                :stationId, :observedAt, :temp, :humidity, :windSpeed, :precip, :createdAt
            )
            on conflict (station_id, observed_at) do nothing
        """,
        nativeQuery = true,
    )
    fun insertIfAbsent(
        @Param("stationId") stationId: Long,
        @Param("observedAt") observedAt: Instant,
        @Param("temp") temp: Float?,
        @Param("humidity") humidity: Float?,
        @Param("windSpeed") windSpeed: Float?,
        @Param("precip") precip: Float?,
        @Param("createdAt") createdAt: Instant,
    ): Int

    @Query("select w from WeatherObservationEntity w where w.stationId = :stationId order by w.observedAt desc limit 1")
    fun findLatest(@Param("stationId") stationId: Long): WeatherObservationEntity?
}

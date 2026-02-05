package lime.atmo.infra.jpa.repo

import lime.atmo.infra.jpa.entity.WeatherForecastEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

interface WeatherForecastRepo : JpaRepository<WeatherForecastEntity, Long> {
    fun findByStationIdAndForecastAtBetweenOrderByForecastAt(
        stationId: Long,
        from: Instant,
        to: Instant,
    ): List<WeatherForecastEntity>

    @Query(
        """
        select w from WeatherForecastEntity w
        where w.stationId = :stationId
        and w.forecastAt = :forecastAt
        """,
    )
    fun findOne(@Param("stationId") stationId: Long, @Param("forecastAt") forecastAt: Instant): WeatherForecastEntity?

    @Modifying
    @Transactional
    @Query("delete from WeatherForecastEntity w where w.forecastAt < :cutoff")
    fun deleteBefore(@Param("cutoff") cutoff: Instant): Int
}

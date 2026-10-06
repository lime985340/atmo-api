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
    @Query(
        value = """
            insert into weather_forecasts (
                station_id, forecast_at, temp, humidity, wind_speed, pop, sky,
                precip_type, precip_mm, created_at
            ) values (
                :stationId, :forecastAt, :temp, :humidity, :windSpeed, :pop, :sky,
                :precipType, :precipMm, :createdAt
            )
            on conflict (station_id, forecast_at) do update set
                temp = excluded.temp,
                humidity = excluded.humidity,
                wind_speed = excluded.wind_speed,
                pop = excluded.pop,
                sky = excluded.sky,
                precip_type = excluded.precip_type,
                precip_mm = excluded.precip_mm
        """,
        nativeQuery = true,
    )
    fun upsert(
        @Param("stationId") stationId: Long,
        @Param("forecastAt") forecastAt: Instant,
        @Param("temp") temp: Float?,
        @Param("humidity") humidity: Float?,
        @Param("windSpeed") windSpeed: Float?,
        @Param("pop") pop: Float?,
        @Param("sky") sky: String?,
        @Param("precipType") precipType: String?,
        @Param("precipMm") precipMm: Float?,
        @Param("createdAt") createdAt: Instant,
    ): Int

    @Modifying
    @Transactional
    @Query("delete from WeatherForecastEntity w where w.forecastAt < :cutoff")
    fun deleteBefore(@Param("cutoff") cutoff: Instant): Int
}

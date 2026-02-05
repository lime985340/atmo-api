package lime.atmos.infra.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(
    name = "weather_forecasts",
    indexes = [
        Index(name = "idx_weather_forecast_station_time", columnList = "station_id,forecast_at"),
        Index(name = "idx_weather_forecast_time", columnList = "forecast_at"),
    ],
)
class WeatherForecastEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "station_id", nullable = false)
    var stationId: Long,

    @Column(name = "forecast_at", nullable = false)
    var forecastAt: Instant,

    var temp: Float? = null,

    var humidity: Float? = null,

    @Column(name = "wind_speed")
    var windSpeed: Float? = null,

    var pop: Float? = null,

    var sky: String? = null,

    @Column(name = "precip_type")
    var precipType: String? = null,

    @Column(name = "precip_mm")
    var precipMm: Float? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),
)

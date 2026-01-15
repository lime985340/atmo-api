package lime.atmos.infra.jpa.entity

import jakarta.persistence.*
import java.io.Serializable
import java.time.Instant

@Embeddable
data class WeatherHourlyId(
    @Column(name = "location_id")
    var locationId: Long = 0,

    @Column(name = "forecast_at")
    var forecastAt: Instant = Instant.now()
) : Serializable

@Entity
@Table(name = "weather_hourly_forecast")
class WeatherHourlyEntity(
    @EmbeddedId
    var id: WeatherHourlyId = WeatherHourlyId(),

    @Column(name = "base_datetime", nullable = false)
    var baseDatetime: Instant = Instant.now(),

    @Column(name = "temp_c")
    var tempC: Double? = null,

    var humidity: Double? = null,

    @Column(name = "wind_speed")
    var windSpeed: Double? = null,

    var pop: Double? = null,
    var sky: String? = null,

    @Column(name = "precip_type")
    var precipType: String? = null,

    @Column(name = "precip_mm")
    var precipMm: Double? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now()
)

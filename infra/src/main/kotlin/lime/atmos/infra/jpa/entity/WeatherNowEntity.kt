package lime.atmos.infra.jpa.entity

import jakarta.persistence.*
import java.io.Serializable
import java.time.Instant

@Embeddable
data class WeatherNowId(
    @Column(name = "location_id")
    var locationId: Long = 0,

    @Column(name = "observed_at")
    var observedAt: Instant = Instant.now()
) : Serializable

@Entity
@Table(name = "weather_now")
class WeatherNowEntity(
    @EmbeddedId
    var id: WeatherNowId = WeatherNowId(),

    @Column(name = "temp_c")
    var tempC: Double? = null,

    var humidity: Double? = null,

    @Column(name = "wind_speed")
    var windSpeed: Double? = null,

    @Column(name = "precip_type")
    var precipType: String? = null,

    @Column(name = "precip_mm")
    var precipMm: Double? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now()
)

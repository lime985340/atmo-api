package lime.atmo.infra.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "weather_currnets")
class StationWeatherCurrentEntity(
    @Id
    @Column(name = "station_id")
    var stationId: Long? = null,

    @Column(name = "obs_time", nullable = false, length = 16)
    var obsTime: String = "",

    @Column(name = "temp")
    var temp: Double? = null,

    @Column(name = "precip")
    var precip: Double? = null,

    @Column(name = "wind_speed")
    var windSpeed: Double? = null,

    @Column(name = "humidity")
    var humidity: Double? = null,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)

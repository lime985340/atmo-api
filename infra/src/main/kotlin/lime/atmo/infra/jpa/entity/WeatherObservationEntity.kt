package lime.atmo.infra.jpa.entity

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
    name = "weather_observations",
    indexes = [
        Index(name = "idx_weather_observation_observed_at", columnList = "observed_at"),
        Index(name = "idx_weather_observation_station_time", columnList = "station_id,observed_at DESC"),
    ],
)
class WeatherObservationEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "station_id", nullable = false)
    var stationId: Long,

    @Column(name = "observed_at", nullable = false)
    var observedAt: Instant,

    var temp: Float? = null,

    var humidity: Float? = null,

    @Column(name = "wind_speed")
    var windSpeed: Float? = null,

    var precip: Float? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),
)

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
    name = "air_observations",
    indexes = [
        Index(name = "idx_air_observation_observed_at", columnList = "observed_at"),
        Index(name = "idx_air_observation_station_time", columnList = "station_id,observed_at DESC"),
    ],
)
class AirObservationEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "station_id", nullable = false)
    var stationId: Long,

    @Column(name = "observed_at", nullable = false)
    var observedAt: Instant,

    var pm10: Float? = null,

    var pm25: Float? = null,

    var o3: Float? = null,

    var no2: Float? = null,

    var co: Float? = null,

    var so2: Float? = null,

    var grade: Int? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),
)

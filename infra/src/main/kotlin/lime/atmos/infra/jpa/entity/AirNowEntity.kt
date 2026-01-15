package lime.atmos.infra.jpa.entity

import jakarta.persistence.*
import java.io.Serializable
import java.time.Instant

@Embeddable
data class AirNowId(
    @Column(name = "station_name")
    var stationName: String = "",

    @Column(name = "measured_at")
    var measuredAt: Instant = Instant.now()
) : Serializable

@Entity
@Table(name = "air_now")
class AirNowEntity(
    @EmbeddedId
    var id: AirNowId = AirNowId(),

    var khai: Double? = null,
    var pm10: Double? = null,
    var pm25: Double? = null,
    var o3: Double? = null,
    var grade: String? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now()
)

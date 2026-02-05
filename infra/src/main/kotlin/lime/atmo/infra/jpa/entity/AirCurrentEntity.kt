package lime.atmo.infra.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(
    name = "air_currents",
    indexes = [Index(name = "idx_air_current_time", columnList = "data_time DESC")],
)
class AirCurrentEntity(
    @Id
    @Column(name = "station_id")
    var stationId: Long,

    @Column(name = "data_time", nullable = false)
    var dataTime: Instant,

    var pm10: Float? = null,

    var pm25: Float? = null,

    var grade: Int? = null,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)

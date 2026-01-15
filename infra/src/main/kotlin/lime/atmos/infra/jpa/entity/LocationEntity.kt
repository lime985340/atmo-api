package lime.atmos.infra.jpa.entity

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "location")
class LocationEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,

    @Column(nullable = false)
    var name: String = "",

    @Column(nullable = false)
    var lat: Double = 0.0,

    @Column(nullable = false)
    var lon: Double = 0.0,

    @Column(nullable = false)
    var nx: Int = 0,

    @Column(nullable = false)
    var ny: Int = 0,

    @Column(name = "station_name", nullable = false)
    var stationName: String = "",

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
)

package lime.atmo.infra.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant

@Entity
@Table(
    name = "stations",
    uniqueConstraints = [UniqueConstraint(name = "uq_station_provider_name_sido", columnNames = ["provider", "station_name", "sido"])],
    indexes = [
        Index(name = "idx_station_sido", columnList = "sido"),
        Index(name = "idx_station_active", columnList = "is_active"),
        Index(name = "idx_station_lat_lng", columnList = "lat,lng"),
    ],
)
class StationEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false)
    var provider: String = "AIRKOREA",

    @Column(name = "station_code")
    var stationCode: String? = null,

    @Column(name = "station_name", nullable = false)
    var stationName: String,

    var addr: String? = null,

    var sido: String? = null,

    @Column(name = "region_code")
    var regionCode: String? = null,

    var lat: Double? = null,

    var lng: Double? = null,

    /**
     * 기상청 격자 좌표(nx, ny)
     * - 제공 API가 nx/ny를 직접 주면 그대로 저장
     * - 아니면 lat/lng를 DFS 격자로 변환해 저장
     */
    @Column(name = "nx")
    var nx: Int? = null,

    @Column(name = "ny")
    var ny: Int? = null,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)

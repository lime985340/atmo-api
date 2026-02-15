package lime.atmo.infra.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "favorite_locations",
    indexes = [
        Index(name = "idx_favorite_locations_user_active", columnList = "user_id,is_active"),
        Index(name = "idx_favorite_locations_nxny", columnList = "nx,ny"),
        Index(name = "idx_favorite_locations_address", columnList = "address"),
    ],
)
class FavoriteLocationEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "user_id", nullable = false)
    var userId: UUID,

    @Column(nullable = false)
    var description: String,

    @Column(nullable = false)
    var lat: Double,

    @Column(name = "lon", nullable = false)
    var lon: Double,

    @Column(nullable = false)
    var nx: Int,

    @Column(nullable = false)
    var ny: Int,

    @Column(name = "address")
    var address: String? = null,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)

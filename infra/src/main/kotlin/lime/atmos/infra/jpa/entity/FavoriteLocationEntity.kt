package lime.atmos.infra.jpa.entity

import jakarta.persistence.*
import java.io.Serializable
import java.time.Instant
import java.util.UUID

@Embeddable
data class FavoriteLocationId(
    @Column(name = "user_id")
    var userId: UUID = UUID.randomUUID(),

    @Column(name = "location_id")
    var locationId: Long = 0
) : Serializable

@Entity
@Table(name = "favorite_location")
class FavoriteLocationEntity(
    @EmbeddedId
    var id: FavoriteLocationId = FavoriteLocationId(),

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now()
)

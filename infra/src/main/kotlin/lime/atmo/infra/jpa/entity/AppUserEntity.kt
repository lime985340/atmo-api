package lime.atmo.infra.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "app_users",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uq_user_provider_sub",
            columnNames = ["provider", "provider_subject"],
        ),
    ],
)
class AppUserEntity(
    @Id
    var id: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    var provider: String = "",

    @Column(name = "provider_subject", nullable = false)
    var providerSubject: String = "",

    var email: String? = null,

    var name: String? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)

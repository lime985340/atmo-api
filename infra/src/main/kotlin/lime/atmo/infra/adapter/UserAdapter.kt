package lime.atmo.infra.adapter

import lime.atmo.application.port.UserInfo
import lime.atmo.application.port.UserPort
import lime.atmo.infra.jpa.entity.AppUserEntity
import lime.atmo.infra.jpa.repo.AppUserRepository
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID

@Component
class UserAdapter(
    private val repo: AppUserRepository,
) : UserPort {
    override fun findById(userId: UUID): UserInfo? {
        return repo.findById(userId).orElse(null)?.let {
            UserInfo(it.id, it.provider, it.providerSubject, it.email, it.name)
        }
    }

    override fun findOrCreateByProvider(
        provider: String,
        providerSubject: String,
        email: String?,
        name: String?,
    ): UserInfo {
        val existing = repo.findByProviderAndProviderSubject(provider, providerSubject)
        val entity = if (existing != null) {
            existing.apply {
                this.email = email ?: this.email
                this.name = name ?: this.name
                this.updatedAt = Instant.now()
            }
        } else {
            AppUserEntity(
                id = UUID.randomUUID(),
                provider = provider,
                providerSubject = providerSubject,
                email = email,
                name = name,
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
            )
        }
        val saved = repo.save(entity)
        return UserInfo(saved.id, saved.provider, saved.providerSubject, saved.email, saved.name)
    }
}

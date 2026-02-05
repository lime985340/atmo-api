package lime.atmos.infra.jpa.repo

import lime.atmos.infra.jpa.entity.AppUserEntity
import lime.atmos.infra.jpa.entity.RefreshTokenEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface AppUserRepository : JpaRepository<AppUserEntity, UUID> {
    fun findByProviderAndProviderSubject(provider: String, providerSubject: String): AppUserEntity?
}

interface RefreshTokenRepository : JpaRepository<RefreshTokenEntity, UUID> {
    fun findByTokenHash(tokenHash: String): RefreshTokenEntity?
}

package lime.atmos.infra.jpa.repo

import lime.atmos.infra.jpa.entity.FavoriteLocationEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

interface FavoriteLocationRepository : JpaRepository<FavoriteLocationEntity, Long> {

    fun findAllByUserIdAndIsActiveTrueOrderByCreatedAtDesc(userId: UUID): List<FavoriteLocationEntity>

    fun findByIdAndUserIdAndIsActiveTrue(id: Long, userId: UUID): FavoriteLocationEntity?

    @Modifying
    @Transactional
    @Query("update FavoriteLocationEntity f set f.isActive=false, f.updatedAt = :updatedAt where f.id=:id and f.userId=:userId and f.isActive=true")
    fun softDelete(
        @Param("id") id: Long,
        @Param("userId") userId: UUID,
        @Param("updatedAt") updatedAt: java.time.Instant,
    ): Int
}

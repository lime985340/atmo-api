package lime.atmos.infra.jpa.repo

import lime.atmos.infra.jpa.entity.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.util.UUID

interface AppUserRepository : JpaRepository<AppUserEntity, UUID> {
    fun findByProviderAndProviderSubject(provider: String, providerSubject: String): AppUserEntity?
}

interface RefreshTokenRepository : JpaRepository<RefreshTokenEntity, UUID> {
    fun findByTokenHash(tokenHash: String): RefreshTokenEntity?
}

interface LocationRepository : JpaRepository<LocationEntity, Long>

interface FavoriteLocationRepository : JpaRepository<FavoriteLocationEntity, FavoriteLocationId> {
    @Query("select f.id.locationId from FavoriteLocationEntity f where f.id.userId = :userId")
    fun listLocationIds(@Param("userId") userId: UUID): List<Long>
}

interface WeatherNowRepository : JpaRepository<WeatherNowEntity, WeatherNowId> {
    @Query("select w from WeatherNowEntity w where w.id.locationId = :locationId order by w.id.observedAt desc limit 1")
    fun findLatest(@Param("locationId") locationId: Long): WeatherNowEntity?
}

interface WeatherHourlyRepository : JpaRepository<WeatherHourlyEntity, WeatherHourlyId> {
    @Query(
        "select w from WeatherHourlyEntity w where w.id.locationId = :locationId and w.id.forecastAt between :from and :to order by w.id.forecastAt asc"
    )
    fun findRange(
        @Param("locationId") locationId: Long,
        @Param("from") from: Instant,
        @Param("to") to: Instant
    ): List<WeatherHourlyEntity>

    @Query("select max(w.baseDatetime) from WeatherHourlyEntity w where w.id.locationId = :locationId")
    fun maxBaseDatetime(@Param("locationId") locationId: Long): Instant?
}

interface AirNowRepository : JpaRepository<AirNowEntity, AirNowId> {
    @Query(
        "select a from AirNowEntity a where a.id.stationName = :stationName order by a.id.measuredAt desc limit 1"
    )
    fun findLatest(@Param("stationName") stationName: String): AirNowEntity?

    @Query("select max(a.id.measuredAt) from AirNowEntity a where a.id.stationName = :stationName")
    fun maxMeasuredAt(@Param("stationName") stationName: String): Instant?
}

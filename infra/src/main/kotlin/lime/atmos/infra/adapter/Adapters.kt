package lime.atmos.infra.adapter

import lime.atmos.application.port.*
import lime.atmos.domain.model.*
import lime.atmos.infra.jpa.entity.*
import lime.atmos.infra.jpa.repo.*
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID

@Component
class LocationAdapter(
    private val repo: LocationRepository
) : LocationPort {
    override fun getLocation(locationId: Long): LocationDto? {
        return repo.findById(locationId).orElse(null)?.let {
            LocationDto(it.id, it.name, it.stationName)
        }
    }
}

@Component
class WeatherReadAdapter(
    private val nowRepo: WeatherNowRepository,
    private val hourlyRepo: WeatherHourlyRepository
) : WeatherReadPort {
    override fun getWeatherNow(locationId: Long): WeatherNow? {
        return nowRepo.findLatest(locationId)?.let {
            WeatherNow(
                it.id.observedAt,
                it.tempC,
                it.humidity,
                it.windSpeed,
                it.precipType,
                it.precipMm
            )
        }
    }

    override fun getNextHours(locationId: Long, from: Instant, to: Instant): List<WeatherHour> {
        return hourlyRepo.findRange(locationId, from, to).map {
            WeatherHour(
                it.id.forecastAt,
                it.tempC,
                it.humidity,
                it.windSpeed,
                it.pop,
                it.sky,
                it.precipType,
                it.precipMm
            )
        }
    }

    override fun getLatestUpdateTime(locationId: Long): Instant? {
        return listOfNotNull(
            nowRepo.findLatest(locationId)?.id?.observedAt,
            hourlyRepo.maxBaseDatetime(locationId)
        ).maxOrNull()
    }
}

@Component
class AirReadAdapter(
    private val repo: AirNowRepository
) : AirReadPort {
    override fun getAirNow(stationName: String): AirNow? {
        return repo.findLatest(stationName)?.let {
            AirNow(
                it.id.measuredAt,
                it.khai,
                it.pm10,
                it.pm25,
                it.o3,
                it.grade
            )
        }
    }

    override fun getLatestUpdateTime(stationName: String): Instant? {
        return repo.maxMeasuredAt(stationName)
    }
}

@Component
class FavoriteAdapter(
    private val repo: FavoriteLocationRepository
) : FavoritePort {
    override fun listFavorites(userId: UUID): List<Long> {
        return repo.listLocationIds(userId)
    }

    override fun addFavorite(userId: UUID, locationId: Long) {
        repo.save(FavoriteLocationEntity(FavoriteLocationId(userId, locationId)))
    }

    override fun removeFavorite(userId: UUID, locationId: Long) {
        repo.deleteById(FavoriteLocationId(userId, locationId))
    }
}

@Component
class UserAdapter(
    private val repo: AppUserRepository
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
        name: String?
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
                updatedAt = Instant.now()
            )
        }
        val saved = repo.save(entity)
        return UserInfo(saved.id, saved.provider, saved.providerSubject, saved.email, saved.name)
    }
}

package lime.atmo.infra.adapter

import lime.atmo.application.port.AirReadPort
import lime.atmo.application.port.FavoriteLocationDto
import lime.atmo.application.port.FavoritePort
import lime.atmo.application.port.LocationDto
import lime.atmo.application.port.LocationPort
import lime.atmo.application.port.UserInfo
import lime.atmo.application.port.UserPort
import lime.atmo.application.port.WeatherReadPort
import lime.atmo.domain.model.AirNow
import lime.atmo.domain.model.WeatherHour
import lime.atmo.domain.model.WeatherNow
import lime.atmo.infra.jpa.entity.AppUserEntity
import lime.atmo.infra.jpa.entity.FavoriteLocationEntity
import lime.atmo.infra.jpa.entity.StationEntity
import lime.atmo.infra.jpa.repo.AirCurrentRepo
import lime.atmo.infra.jpa.repo.AppUserRepository
import lime.atmo.infra.jpa.repo.FavoriteLocationRepository
import lime.atmo.infra.jpa.repo.StationRepo
import lime.atmo.infra.jpa.repo.StationWeatherCurrentRepo
import lime.atmo.infra.jpa.repo.WeatherForecastRepo
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID

@Component
class LocationAdapter(
    private val repo: FavoriteLocationRepository,
) : LocationPort {
    override fun getLocation(locationId: Long): LocationDto? {
        return repo.findById(locationId).orElse(null)?.let {
            LocationDto(it.id!!, it.name, it.stationId, it.stationName)
        }
    }
}

@Component
class WeatherReadAdapter(
    private val stationWeatherRepo: StationWeatherCurrentRepo,
    private val forecastRepo: WeatherForecastRepo,
) : WeatherReadPort {
    override fun getWeatherNow(stationId: Long): WeatherNow? {
        return stationWeatherRepo.findByStationId(stationId)?.let {
            WeatherNow(
                parseObs(it.obsTime),
                it.temp,
                it.humidity,
                it.wind,
                null,
                it.rain,
            )
        }
    }

    override fun getNextHours(stationId: Long, from: Instant, to: Instant): List<WeatherHour> {
        return forecastRepo.findByStationIdAndForecastAtBetweenOrderByForecastAt(stationId, from, to)
            .map {
                WeatherHour(
                    forecastAt = it.forecastAt,
                    tempC = it.temp?.toDouble(),
                    humidity = it.humidity?.toDouble(),
                    windSpeed = it.windSpeed?.toDouble(),
                    pop = it.pop?.toDouble(),
                    sky = it.sky,
                    precipType = it.precipType,
                    precipMm = it.precipMm?.toDouble(),
                )
            }
    }

    override fun getLatestUpdateTime(stationId: Long): Instant? {
        return stationWeatherRepo.findByStationId(stationId)?.updatedAt
    }

    private fun parseObs(raw: String): Instant {
        return try {
            val parts = raw.trim().split(" ")
            val date = parts.getOrNull(0) ?: return Instant.now()
            val time = parts.getOrNull(1) ?: "0000"
            val ldt = java.time.LocalDateTime.parse(date + time, java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmm"))
            ldt.atZone(java.time.ZoneId.of("Asia/Seoul")).toInstant()
        } catch (e: Exception) {
            Instant.now()
        }
    }
}

@Component
class AirReadAdapter(
    private val repo: AirCurrentRepo,
) : AirReadPort {
    override fun getAirNow(stationId: Long): AirNow? {
        return repo.findByStationId(stationId)?.let {
            AirNow(
                it.dataTime,
                null,
                it.pm10?.toDouble(),
                it.pm25?.toDouble(),
                null,
                it.grade,
            )
        }
    }

    override fun getLatestUpdateTime(stationId: Long): Instant? {
        return repo.findByStationId(stationId)?.dataTime
    }
}

@Component
class FavoriteAdapter(
    private val favRepo: FavoriteLocationRepository,
    private val stationRepo: StationRepo,
) : FavoritePort {

    override fun listFavorites(userId: UUID): List<FavoriteLocationDto> {
        return favRepo.findAllByUserIdAndIsActiveTrueOrderByCreatedAtDesc(userId).map {
            FavoriteLocationDto(
                id = it.id!!,
                name = it.name,
                lat = it.lat,
                lng = it.lon,
                stationId = it.stationId,
                stationName = it.stationName,
            )
        }
    }

    override fun createFavorite(userId: UUID, name: String, lat: Double, lng: Double): FavoriteLocationDto {
        val nearest = findNearestStation(lat, lng)
        val nxny = nearest?.let { s ->
            val nx = s.nx
            val ny = s.ny
            if (nx != null && ny != null) Pair(nx, ny) else null
        } ?: run {
            val g = GridConverter.toGrid(lat, lng)
            Pair(g.first, g.second)
        }

        val entity = FavoriteLocationEntity(
            userId = userId,
            name = name,
            lat = lat,
            lon = lng,
            nx = nxny.first,
            ny = nxny.second,
            stationId = nearest?.id,
            stationName = nearest?.stationName,
            isActive = true,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        )
        val saved = favRepo.save(entity)
        return FavoriteLocationDto(saved.id!!, saved.name, saved.lat, saved.lon, saved.stationId, saved.stationName)
    }

    override fun removeFavorite(userId: UUID, favoriteId: Long) {
        val updated = favRepo.softDelete(favoriteId, userId, Instant.now())
        if (updated == 0) throw IllegalArgumentException("favorite not found")
    }

    private fun findNearestStation(lat: Double, lng: Double): StationEntity? {
        // 1) candidate bbox (expand gradually)
        val steps = listOf(0.10, 0.25, 0.50, 1.00) // degrees
        for (d in steps) {
            val candidates = stationRepo.findActiveInBbox(lat - d, lat + d, lng - d, lng + d)
            val nearest = candidates.minByOrNull { s ->
                val slat = s.lat ?: return@minByOrNull Double.POSITIVE_INFINITY
                val slng = s.lng ?: return@minByOrNull Double.POSITIVE_INFINITY
                haversineKm(lat, lng, slat, slng)
            }
            if (nearest != null) return nearest
        }
        // last resort: all active with coords (could be large)
        return stationRepo.findAllActiveWithCoordinates().minByOrNull { s ->
            val slat = s.lat ?: return@minByOrNull Double.POSITIVE_INFINITY
            val slng = s.lng ?: return@minByOrNull Double.POSITIVE_INFINITY
            haversineKm(lat, lng, slat, slng)
        }
    }

    private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0088
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
            Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }

    private object GridConverter {
        // KMA DFS(LCC) grid conversion (lat,lng -> nx,ny), same constants as KMA sample.
        private const val RE = 6371.00877
        private const val GRID = 5.0
        private const val SLAT1 = 30.0
        private const val SLAT2 = 60.0
        private const val OLON = 126.0
        private const val OLAT = 38.0
        private const val XO = 43.0
        private const val YO = 136.0

        fun toGrid(lat: Double, lng: Double): Pair<Int, Int> {
            val degToRad = Math.PI / 180.0
            val re = RE / GRID
            val slat1 = SLAT1 * degToRad
            val slat2 = SLAT2 * degToRad
            val olon = OLON * degToRad
            val olat = OLAT * degToRad

            var sn = Math.tan(Math.PI * 0.25 + slat2 * 0.5) / Math.tan(Math.PI * 0.25 + slat1 * 0.5)
            sn = Math.log(Math.cos(slat1) / Math.cos(slat2)) / Math.log(sn)
            var sf = Math.tan(Math.PI * 0.25 + slat1 * 0.5)
            sf = Math.pow(sf, sn) * Math.cos(slat1) / sn
            var ro = Math.tan(Math.PI * 0.25 + olat * 0.5)
            ro = re * sf / Math.pow(ro, sn)

            var ra = Math.tan(Math.PI * 0.25 + (lat) * degToRad * 0.5)
            ra = re * sf / Math.pow(ra, sn)
            var theta = (lng) * degToRad - olon
            if (theta > Math.PI) theta -= 2.0 * Math.PI
            if (theta < -Math.PI) theta += 2.0 * Math.PI
            theta *= sn

            val x = (ra * Math.sin(theta) + XO + 0.5).toInt()
            val y = (ro - ra * Math.cos(theta) + YO + 0.5).toInt()
            return Pair(x, y)
        }
    }
}

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

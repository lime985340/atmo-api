package lime.atmos.ingestion

import lime.atmos.common.config.ForecastRetentionProps
import lime.atmos.common.config.PublicDataProps
import lime.atmos.infra.jpa.entity.AirCurrentEntity
import lime.atmos.infra.jpa.entity.AirObservationEntity
import lime.atmos.infra.jpa.entity.IngestionRunEntity
import lime.atmos.infra.jpa.entity.StationEntity
import lime.atmos.infra.jpa.entity.StationWeatherCurrentEntity
import lime.atmos.infra.jpa.entity.WeatherForecastEntity
import lime.atmos.infra.jpa.entity.WeatherObservationEntity
import lime.atmos.infra.jpa.repo.AirCurrentRepo
import lime.atmos.infra.jpa.repo.AirObservationRepo
import lime.atmos.infra.jpa.repo.IngestionRunRepo
import lime.atmos.infra.jpa.repo.StationRepo
import lime.atmos.infra.jpa.repo.StationWeatherCurrentRepo
import lime.atmos.infra.jpa.repo.WeatherForecastRepo
import lime.atmos.infra.jpa.repo.WeatherObservationRepo
import lime.atmos.ingestion.kma.GridConverter
import lime.atmos.ingestion.kma.KmaFcstItem
import lime.atmos.ingestion.kma.KmaForecastClient
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * One-shot ingestion runner.
 * Run with: --spring.profiles.active=ingestion
 *
 * - AIR: updates air_current for all configured sido names
 * - WEATHER: updates station_weather_current for all active stations
 * - STATION_SEED (optional): seeds/updates station meta using getMsrstnList
 */
@Profile("ingestion")
@Component
class IngestionRunner(
    private val props: PublicDataProps,
    private val stationRepo: StationRepo,
    private val airCurrentRepo: AirCurrentRepo,
    private val airObservationRepo: AirObservationRepo,
    private val weatherObservationRepo: WeatherObservationRepo,
    private val stationWeatherCurrentRepo: StationWeatherCurrentRepo,
    private val weatherForecastRepo: WeatherForecastRepo,
    private val forecastClient: KmaForecastClient,
    private val retentionProps: ForecastRetentionProps,
    private val runRepo: IngestionRunRepo,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * AirKorea - 대기오염정보 조회 서비스 (측정값)
     */
    private val airClient = WebClient.builder()
        .baseUrl("https://apis.data.go.kr/B552584/ArpltnInforInqireSvc")
        .build()

    /**
     * AirKorea - 측정소 정보 조회 서비스 (측정소 메타/좌표)
     * getMsrstnList 는 MsrstnInfoInqireSvc 에 속한다.
     */
    private val stationClient = WebClient.builder()
        .baseUrl("https://apis.data.go.kr/B552584/MsrstnInfoInqireSvc")
        .build()

    private val kmaClient = WebClient.builder()
        .baseUrl("https://apis.data.go.kr/1360000/VilageFcstInfoService_2.0")
        .build()

    fun seedStationsIfMissing() {
        if (props.serviceKey.isBlank()) {
            log.warn("PublicData serviceKey is blank; skip station seeding.")
            return
        }

        val run = runRepo.save(IngestionRunEntity(jobType = "STATION_SEED", status = "SUCCESS"))
        val started = Instant.now()
        var ok = 0
        var fail = 0
        var lastErr: String? = null

        // Minimal default list (can be expanded later)
        val sidos = listOf("서울", "부산", "대구", "인천", "광주", "대전", "울산", "세종", "경기", "강원", "충북", "충남", "전북", "전남", "경북", "경남", "제주")

        try {
            for (sido in sidos) {
                val res = stationClient.get()
                    .uri { b ->
                        b.path("/getMsrstnList")
                            .queryParam("serviceKey", props.serviceKey)
                            .queryParam("returnType", "json")
                            .queryParam("numOfRows", 1000)
                            .queryParam("pageNo", 1)
                            .queryParam("addr", sido)
                            .build()
                    }
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(Map::class.java)
                    .block()

                val items = (((res?.get("response") as? Map<*, *>)?.get("body") as? Map<*, *>)?.get("items") as? List<*>) ?: emptyList<Any>()
                for (it in items) {
                    val m = it as? Map<*, *> ?: continue
                    val stationName = m["stationName"]?.toString() ?: continue
                    val addr = m["addr"]?.toString()
                    val stationCode = m["stationCode"]?.toString()
                    val regionCode = m["regionCode"]?.toString()

                    // ⚠️ 중요: 공공데이터 API의 dmX는 위도(latitude), dmY는 경도(longitude)
                    // 일반적인 XY 좌표와 반대로 명명되어 있으므로 주의!
                    val lat = m["dmX"]?.toString()?.toDoubleOrNull() ?: m["tmX"]?.toString()?.toDoubleOrNull()
                    val lng = m["dmY"]?.toString()?.toDoubleOrNull() ?: m["tmY"]?.toString()?.toDoubleOrNull()
                    val nxFromApi = m["nx"]?.toString()?.toIntOrNull()
                    val nyFromApi = m["ny"]?.toString()?.toIntOrNull()

                    val grid = if (nxFromApi != null && nyFromApi != null) {
                        GridConverter.Grid(nxFromApi, nyFromApi)
                    } else if (lat != null && lng != null) {
                        GridConverter.toGrid(lat, lng)
                    } else {
                        null
                    }

                    val existing = stationRepo.findByProviderAndStationNameAndSido("AIRKOREA", stationName, sido)
                    val entity = existing?.apply {
                        this.stationCode = stationCode ?: this.stationCode
                        this.addr = addr ?: this.addr
                        this.lat = lat ?: this.lat
                        this.lng = lng ?: this.lng
                        this.regionCode = regionCode ?: this.regionCode
                        if (grid != null) {
                            this.nx = grid.nx
                            this.ny = grid.ny
                        }
                        this.isActive = true
                        this.updatedAt = Instant.now()
                    } ?: StationEntity(
                        provider = "AIRKOREA",
                        stationCode = stationCode,
                        stationName = stationName,
                        addr = addr,
                        sido = sido,
                        regionCode = regionCode,
                        lat = lat,
                        lng = lng,
                        nx = grid?.nx,
                        ny = grid?.ny,
                        isActive = true,
                        createdAt = Instant.now(),
                        updatedAt = Instant.now(),
                    )
                    stationRepo.save(entity)
                    ok++
                }
            }
        } catch (e: Exception) {
            log.error("station seed failed", e)
            fail++
            lastErr = e.message
        } finally {
            run.status = if (fail > 0) "FAILED" else "SUCCESS"
            run.startedAt = started
            run.endedAt = Instant.now()
            run.successCount = ok
            run.failCount = fail
            run.lastError = lastErr
            run.updatedAt = Instant.now()
            runRepo.save(run)
        }
    }

    fun ingestAir() {
        if (props.serviceKey.isBlank()) {
            log.warn("PublicData serviceKey is blank; skip air ingestion.")
            return
        }

        val run = runRepo.save(IngestionRunEntity(jobType = "AIR", status = "SUCCESS"))
        val started = Instant.now()
        var ok = 0
        var fail = 0
        var lastErr: String? = null

        val sidos = listOf("서울", "부산", "대구", "인천", "광주", "대전", "울산", "세종", "경기", "강원", "충북", "충남", "전북", "전남", "경북", "경남", "제주")

        try {
            for (sido in sidos) {
                val res = airClient.get()
                    .uri { b ->
                        b.path("/getCtprvnRltmMesureDnsty")
                            .queryParam("serviceKey", props.serviceKey)
                            .queryParam("returnType", "json")
                            .queryParam("numOfRows", 1000)
                            .queryParam("pageNo", 1)
                            .queryParam("sidoName", sido)
                            .queryParam("ver", "1.0")
                            .build()
                    }
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(Map::class.java)
                    .block()

                val items = (((res?.get("response") as? Map<*, *>)?.get("body") as? Map<*, *>)?.get("items") as? List<*>) ?: emptyList<Any>()
                for (it in items) {
                    val m = it as? Map<*, *> ?: continue
                    val stationName = m["stationName"]?.toString() ?: continue
                    val dataTimeStr = m["dataTime"]?.toString() ?: continue

                    val dataTime = parseAirDataTime(dataTimeStr)
                    val pm10 = m["pm10Value"]?.toString()?.toFloatOrNull()
                    val pm25 = m["pm25Value"]?.toString()?.toFloatOrNull()
                    val o3 = m["o3Value"]?.toString()?.toFloatOrNull()
                    val no2 = m["no2Value"]?.toString()?.toFloatOrNull()
                    val co = m["coValue"]?.toString()?.toFloatOrNull()
                    val so2 = m["so2Value"]?.toString()?.toFloatOrNull()
                    val grade = m["pm10Grade"]?.toString()?.toIntOrNull()

                    // ensure station exists (lat/lng can be filled by seeding later)
                    val station = stationRepo.findByProviderAndStationNameAndSido("AIRKOREA", stationName, sido)
                        ?: stationRepo.save(
                            StationEntity(
                                provider = "AIRKOREA",
                                stationName = stationName,
                                sido = sido,
                                isActive = true,
                                createdAt = Instant.now(),
                                updatedAt = Instant.now(),
                            ),
                        )

                    val stationId = station.id ?: continue
                    val existingObs = airObservationRepo.findByStationIdAndObservedAt(stationId, dataTime)
                    if (existingObs == null) {
                        airObservationRepo.save(
                            AirObservationEntity(
                                stationId = stationId,
                                observedAt = dataTime,
                                pm10 = pm10,
                                pm25 = pm25,
                                o3 = o3,
                                no2 = no2,
                                co = co,
                                so2 = so2,
                                grade = grade,
                                createdAt = Instant.now(),
                            ),
                        )
                    }
                    airCurrentRepo.save(
                        AirCurrentEntity(
                            stationId = stationId,
                            dataTime = dataTime,
                            pm10 = pm10,
                            pm25 = pm25,
                            grade = grade,
                            updatedAt = Instant.now(),
                        ),
                    )
                    ok++
                }
            }
        } catch (e: Exception) {
            log.error("air ingestion failed", e)
            fail++
            lastErr = e.message
        } finally {
            run.status = if (fail > 0) "FAILED" else "SUCCESS"
            run.startedAt = started
            run.endedAt = Instant.now()
            run.successCount = ok
            run.failCount = fail
            run.lastError = lastErr
            run.updatedAt = Instant.now()
            runRepo.save(run)
        }
    }

    fun ingestWeather() {
        if (props.serviceKey.isBlank()) {
            log.warn("PublicData serviceKey is blank; skip weather ingestion.")
            return
        }

        val run = runRepo.save(IngestionRunEntity(jobType = "WEATHER", status = "SUCCESS"))
        val started = Instant.now()
        var ok = 0
        var fail = 0
        var lastErr: String? = null

        val base = computeKmaBase(ZoneId.of("Asia/Seoul"))

        try {
            // v1 기본 UX: "측정소 마커"에서 날씨까지 같이 보여주기 위해, 측정소 좌표(nx,ny)를 기준으로 날씨를 적재한다.
            val stations = stationRepo.findAll().filter { it.isActive }
            for (st in stations) {
                val (nx, ny) = when {
                    st.nx != null && st.ny != null -> st.nx!! to st.ny!!
                    st.lat != null && st.lng != null -> {
                        val g = GridConverter.toGrid(st.lat!!, st.lng!!)
                        st.nx = g.nx
                        st.ny = g.ny
                        stationRepo.save(st)
                        g.nx to g.ny
                    }
                    else -> {
                        fail++
                        lastErr = "station ${st.id} missing nx/ny and lat/lng"
                        continue
                    }
                }
                val res = kmaClient.get()
                    .uri { b ->
                        b.path("/getUltraSrtNcst")
                            .queryParam("serviceKey", props.serviceKey)
                            .queryParam("pageNo", 1)
                            .queryParam("numOfRows", 1000)
                            .queryParam("dataType", "JSON")
                            .queryParam("base_date", base.baseDate)
                            .queryParam("base_time", base.baseTime)
                            .queryParam("nx", nx)
                            .queryParam("ny", ny)
                            .build()
                    }
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(Map::class.java)
                    .block()

                val items = (
                    ((((res?.get("response") as? Map<*, *>)?.get("body") as? Map<*, *>)?.get("items") as? Map<*, *>)?.get("item") as? List<*>)
                        ?: emptyList<Any>()
                    )

                val byCat = items.mapNotNull { it as? Map<*, *> }.associateBy { it["category"]?.toString().orEmpty() }
                val obsDate = byCat["T1H"]?.get("baseDate")?.toString() ?: base.baseDate
                val obsTime = byCat["T1H"]?.get("baseTime")?.toString() ?: base.baseTime
                val obsInstant = parseKmaObs(obsDate, obsTime)

                val tempC = byCat["T1H"]?.get("obsrValue")?.toString()?.toFloatOrNull()
                val humidity = byCat["REH"]?.get("obsrValue")?.toString()?.toFloatOrNull()
                val windSpeed = byCat["WSD"]?.get("obsrValue")?.toString()?.toFloatOrNull()
                val precipMm = byCat["RN1"]?.get("obsrValue")?.toString()?.toFloatOrNull()

                val existingObs = st.id?.let { weatherObservationRepo.findByStationIdAndObservedAt(it, obsInstant) }
                if (existingObs == null && st.id != null) {
                    weatherObservationRepo.save(
                        WeatherObservationEntity(
                            stationId = st.id!!,
                            observedAt = obsInstant,
                            temp = tempC,
                            humidity = humidity,
                            windSpeed = windSpeed,
                            precip = precipMm,
                            createdAt = Instant.now(),
                        ),
                    )
                }
                stationWeatherCurrentRepo.save(
                    StationWeatherCurrentEntity(
                        stationId = st.id,
                        obsTime = "$obsDate $obsTime",
                        temp = tempC?.toDouble(),
                        rain = precipMm?.toDouble(),
                        wind = windSpeed?.toDouble(),
                        humidity = humidity?.toDouble(),
                        updatedAt = Instant.now(),
                    ),
                )
                ok++
            }
        } catch (e: Exception) {
            log.error("weather ingestion failed", e)
            fail++
            lastErr = e.message
        } finally {
            run.status = if (fail > 0) "FAILED" else "SUCCESS"
            run.startedAt = started
            run.endedAt = Instant.now()
            run.successCount = ok
            run.failCount = fail
            run.lastError = lastErr
            run.updatedAt = Instant.now()
            runRepo.save(run)
        }
    }

    fun ingestForecast() {
        if (props.serviceKey.isBlank()) {
            log.warn("PublicData serviceKey is blank; skip forecast ingestion.")
            return
        }

        val run = runRepo.save(IngestionRunEntity(jobType = "FORECAST", status = "SUCCESS"))
        val started = Instant.now()
        var ok = 0
        var fail = 0
        var lastErr: String? = null

        val base = computeKmaBase(ZoneId.of("Asia/Seoul"))

        try {
            val stations = stationRepo.findAll().filter { it.isActive }
            for (st in stations) {
                val (nx, ny) = when {
                    st.nx != null && st.ny != null -> st.nx!! to st.ny!!
                    st.lat != null && st.lng != null -> {
                        val g = GridConverter.toGrid(st.lat!!, st.lng!!)
                        st.nx = g.nx
                        st.ny = g.ny
                        stationRepo.save(st)
                        g.nx to g.ny
                    }
                    else -> {
                        fail++
                        lastErr = "station ${st.id} missing nx/ny and lat/lng"
                        continue
                    }
                }

                val items = forecastClient.fetchUltraSrtFcst(nx, ny, base.baseDate, base.baseTime)
                if (items.isEmpty()) {
                    continue
                }

                val grouped = items.groupBy { it.fcstDate + it.fcstTime }
                for (group in grouped.values) {
                    val data = group.associateBy { it.category }
                    val whenAt = parseKmaForecast(group)

                    if (st.id == null) continue
                    val existing = weatherForecastRepo.findOne(st.id!!, whenAt)

                    val temp = data["T1H"]?.fcstValue?.toFloatOrNull()
                    val humidity = data["REH"]?.fcstValue?.toFloatOrNull()
                    val wind = data["WSD"]?.fcstValue?.toFloatOrNull()
                    val pop = data["POP"]?.fcstValue?.toFloatOrNull()
                    val sky = data["SKY"]?.fcstValue
                    val precipType = data["PTY"]?.fcstValue
                    val precipMm = data["RN1"]?.fcstValue?.toFloatOrNull()

                    if (existing == null) {
                        weatherForecastRepo.save(
                            WeatherForecastEntity(
                                stationId = st.id!!,
                                forecastAt = whenAt,
                                temp = temp,
                                humidity = humidity,
                                windSpeed = wind,
                                pop = pop,
                                sky = sky,
                                precipType = precipType,
                                precipMm = precipMm,
                                createdAt = Instant.now(),
                            ),
                        )
                    } else {
                        existing.temp = temp
                        existing.humidity = humidity
                        existing.windSpeed = wind
                        existing.pop = pop
                        existing.sky = sky
                        existing.precipType = precipType
                        existing.precipMm = precipMm
                        weatherForecastRepo.save(existing)
                    }
                    ok++
                }
            }
        } catch (e: Exception) {
            log.error("forecast ingestion failed", e)
            fail++
            lastErr = e.message
        } finally {
            run.status = if (fail > 0) "FAILED" else "SUCCESS"
            run.startedAt = started
            run.endedAt = Instant.now()
            run.successCount = ok
            run.failCount = fail
            run.lastError = lastErr
            run.updatedAt = Instant.now()
            runRepo.save(run)
        }
    }

    fun cleanupForecasts() {
        val days = retentionProps.retentionDays
        if (days <= 0) return
        val cutoff = Instant.now().minusSeconds(days * 24 * 60 * 60)
        val deleted = weatherForecastRepo.deleteBefore(cutoff)
        log.info("forecast cleanup deleted {} rows before {}", deleted, cutoff)
    }

    private data class KmaBase(val baseDate: String, val baseTime: String)

    private fun computeKmaBase(zone: ZoneId): KmaBase {
        // UltraSrtNcst is published multiple times; to be safe, use "previous hour" as base_time.
        val now = ZonedDateTime.now(zone)
        val base = now.minusMinutes(30) // avoid edge around publish time
        val baseDate = base.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        val baseTime = base.withMinute(0).withSecond(0).format(DateTimeFormatter.ofPattern("HHmm"))
        return KmaBase(baseDate, baseTime)
    }

    private fun parseAirDataTime(raw: String): Instant {
        // "yyyy-MM-dd HH:mm" in KST
        val ldt = LocalDateTime.parse(raw, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
        return ldt.atZone(ZoneId.of("Asia/Seoul")).toInstant()
    }

    private fun parseKmaObs(date: String, time: String): Instant {
        val ldt = LocalDateTime.parse(date + time, DateTimeFormatter.ofPattern("yyyyMMddHHmm"))
        return ldt.atZone(ZoneId.of("Asia/Seoul")).toInstant()
    }

    private fun parseKmaForecast(items: List<KmaFcstItem>): Instant {
        val any = items.firstOrNull() ?: return Instant.now()
        val ldt = LocalDateTime.parse(any.fcstDate + any.fcstTime, DateTimeFormatter.ofPattern("yyyyMMddHHmm"))
        return ldt.atZone(ZoneId.of("Asia/Seoul")).toInstant()
    }
}

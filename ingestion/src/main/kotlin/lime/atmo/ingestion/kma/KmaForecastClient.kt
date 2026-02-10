package lime.atmo.ingestion.kma

import lime.atmo.common.config.PublicDataProps
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

data class KmaFcstItem(
    val category: String,
    val fcstDate: String,
    val fcstTime: String,
    val fcstValue: String,
)

interface KmaForecastClient {
    fun fetchUltraSrtFcst(nx: Int, ny: Int, baseDate: String, baseTime: String): List<KmaFcstItem>
}

@Component
class WebKmaForecastClient(
    private val props: PublicDataProps,
) : KmaForecastClient {

    private val log = LoggerFactory.getLogger(javaClass)

    private val client = WebClient.builder()
        .baseUrl("https://apis.data.go.kr/1360000/VilageFcstInfoService_2.0")
        .build()

    override fun fetchUltraSrtFcst(nx: Int, ny: Int, baseDate: String, baseTime: String): List<KmaFcstItem> {
        if (props.serviceKey.isBlank()) return emptyList()

        val res = KmaRetry.call(
            log = log,
            op = "getUltraSrtFcst(nx=$nx, ny=$ny, base=$baseDate $baseTime)",
        ) {
            client.get()
                .uri { b ->
                    b.path("/getUltraSrtFcst")
                        .queryParam("serviceKey", props.serviceKey)
                        .queryParam("pageNo", 1)
                        .queryParam("numOfRows", 1000)
                        .queryParam("dataType", "JSON")
                        .queryParam("base_date", baseDate)
                        .queryParam("base_time", baseTime)
                        .queryParam("nx", nx)
                        .queryParam("ny", ny)
                        .build()
                }
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(Map::class.java)
                .block()
        }

        val items = (
            ((((res?.get("response") as? Map<*, *>)?.get("body") as? Map<*, *>)?.get("items") as? Map<*, *>)?.get("item") as? List<*>)
                ?: emptyList<Any>()
            )

        return items.mapNotNull { raw ->
            val m = raw as? Map<*, *> ?: return@mapNotNull null
            val category = m["category"]?.toString() ?: return@mapNotNull null
            val fcstDate = m["fcstDate"]?.toString() ?: return@mapNotNull null
            val fcstTime = m["fcstTime"]?.toString() ?: return@mapNotNull null
            val fcstValue = m["fcstValue"]?.toString() ?: return@mapNotNull null
            KmaFcstItem(category, fcstDate, fcstTime, fcstValue)
        }.also {
            if (it.isEmpty()) {
                log.warn("KMA forecast response empty (nx={}, ny={}, base={} {})", nx, ny, baseDate, baseTime)
            }
        }
    }
}

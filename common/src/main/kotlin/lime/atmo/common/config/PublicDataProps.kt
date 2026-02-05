package lime.atmo.common.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.external.publicdata")
data class PublicDataProps(
    /**
     * 공공데이터포털 서비스키
     *
     * - API별로 '활용 신청'은 각각 해야 하지만, 발급되는 serviceKey 문자열은 보통 동일하게 재사용됩니다.
     * - 사용자는 하나의 키로 통일해서 관리하길 원하므로 단일 키로 받습니다.
     */
    val serviceKey: String = "",
) {
    // (legacy holder removed)
}

package lime.atmos.api.oauth

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@Component
class OAuthClient {
    private val web = WebClient.builder().build()
    private val mapper = jacksonObjectMapper()

    fun exchangeCodeForToken(provider: OAuthProviderProps, code: String): Mono<JsonNode> {
        val form = LinkedMultiValueMap<String, String>().apply {
            add("grant_type", "authorization_code")
            add("client_id", provider.clientId)
            if (provider.clientSecret.isNotBlank()) add("client_secret", provider.clientSecret)
            add("redirect_uri", provider.redirectUri)
            add("code", code)
        }

        return web.post()
            .uri(provider.tokenUri)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .accept(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromFormData(form))
            .retrieve()
            .bodyToMono(String::class.java)
            .map { mapper.readTree(it) }
    }

    fun fetchUserInfo(provider: OAuthProviderProps, accessToken: String): Mono<JsonNode> =
        web.get()
            .uri(provider.userinfoUri)
            .header("Authorization", "Bearer $accessToken")
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToMono(String::class.java)
            .map { mapper.readTree(it) }
}

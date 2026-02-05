package lime.atmo.api.security
import org.springframework.stereotype.Component
import java.security.MessageDigest
import java.util.Base64

@Component
class TokenHash {
    fun sha256Base64(input: String): String {
        val d = MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray())

        return Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(d)
    }
}

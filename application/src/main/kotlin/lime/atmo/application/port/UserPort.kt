package lime.atmo.application.port

import java.util.UUID

data class UserInfo(
    val id: UUID,
    val provider: String,
    val providerSubject: String,
    val email: String?,
    val name: String?,
)

interface UserPort {
    fun findById(userId: UUID): UserInfo?

    fun findOrCreateByProvider(
        provider: String,
        providerSubject: String,
        email: String?,
        name: String?,
    ): UserInfo
}

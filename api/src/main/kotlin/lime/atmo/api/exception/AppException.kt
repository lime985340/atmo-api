package lime.atmo.api.exception

import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.HttpStatus.UNAUTHORIZED

sealed class AppException(
    message: String,
    val status: HttpStatus,
) : RuntimeException(message)

class NotFoundException(message: String) : AppException(message, NOT_FOUND)
class UnauthorizedException(message: String) : AppException(message, UNAUTHORIZED)
class BadRequestException(message: String) : AppException(message, BAD_REQUEST)

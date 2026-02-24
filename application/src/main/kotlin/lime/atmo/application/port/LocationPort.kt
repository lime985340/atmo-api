package lime.atmo.application.port

data class LocationDto(
    val id: Long,
    val description: String,
    val address: String?,
    val lat: Double,
    val lng: Double,
)

interface LocationPort {
    fun getLocation(locationId: Long): LocationDto?
    fun findNearestStationId(lat: Double, lng: Double): Long?
}

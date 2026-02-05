package lime.atmo.ingestion.kma

import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.tan

/**
 * KMA DFS(LCC) grid conversion (lat,lng -> nx,ny)
 * Constants are the commonly used values from KMA sample code.
 */
object GridConverter {
    data class Grid(val nx: Int, val ny: Int)

    private const val RE = 6371.00877
    private const val GRID = 5.0
    private const val SLAT1 = 30.0
    private const val SLAT2 = 60.0
    private const val OLON = 126.0
    private const val OLAT = 38.0
    private const val XO = 43.0
    private const val YO = 136.0

    fun toGrid(lat: Double, lng: Double): Grid {
        val degToRad = Math.PI / 180.0
        val re = RE / GRID
        val slat1 = SLAT1 * degToRad
        val slat2 = SLAT2 * degToRad
        val olon = OLON * degToRad
        val olat = OLAT * degToRad

        val sn = ln(cos(slat1) / cos(slat2)) / ln(tan(Math.PI * 0.25 + slat2 * 0.5) / tan(Math.PI * 0.25 + slat1 * 0.5))
        val sf = tan(Math.PI * 0.25 + slat1 * 0.5).pow(sn) * cos(slat1) / sn
        val ro = re * sf / tan(Math.PI * 0.25 + olat * 0.5).pow(sn)

        val ra = re * sf / tan(Math.PI * 0.25 + lat * degToRad * 0.5).pow(sn)
        var theta = lng * degToRad - olon
        if (theta > Math.PI) theta -= 2.0 * Math.PI
        if (theta < -Math.PI) theta += 2.0 * Math.PI
        theta *= sn

        val x = ra * sin(theta) + XO
        val y = ro - ra * cos(theta) + YO

        return Grid(floor(x + 0.5).toInt(), floor(y + 0.5).toInt())
    }
}

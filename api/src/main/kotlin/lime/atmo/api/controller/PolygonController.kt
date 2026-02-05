package lime.atmo.api.controller

import org.springframework.core.io.ClassPathResource
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/map/polygons")
class PolygonController {

    private fun readGeoJson(path: String): String {
        val resource = ClassPathResource(path)
        if (!resource.exists()) {
            return """{"type":"FeatureCollection","features":[]}"""
        }
        return resource.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
    }

    @GetMapping("/sido", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun sido(): ResponseEntity<String> {
        val body = readGeoJson("geojson/sido.json")
        return ResponseEntity.ok(body)
    }

    @GetMapping("/sigungu/{sidoCode}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun sigungu(@PathVariable sidoCode: String): ResponseEntity<String> {
        val body = readGeoJson("geojson/sigungu/$sidoCode.json")
        return ResponseEntity.ok(body)
    }

    @GetMapping("/emd/{sigunguCode}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun emd(@PathVariable sigunguCode: String): ResponseEntity<String> {
        val body = readGeoJson("geojson/emd/$sigunguCode.json")
        return ResponseEntity.ok(body)
    }
}

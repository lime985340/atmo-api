package lime.atmo.api.controller

import lime.atmo.application.usecase.GetDashboardUseCase
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1")
class DashboardController(
    private val uc: GetDashboardUseCase,
) {
    @GetMapping("/dashboard")
    fun dashboard(@RequestParam locationId: Long) = uc.execute(locationId)
}

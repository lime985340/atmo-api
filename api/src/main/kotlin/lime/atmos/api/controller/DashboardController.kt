package lime.atmos.api.controller

import lime.atmos.application.usecase.GetDashboardUseCase
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1")
class DashboardController(
    private val uc: GetDashboardUseCase
) {
    @GetMapping("/dashboard")
    fun dashboard(@RequestParam locationId: Long) = uc.execute(locationId)
}

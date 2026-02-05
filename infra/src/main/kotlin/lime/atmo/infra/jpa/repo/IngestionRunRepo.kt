package lime.atmo.infra.jpa.repo

import lime.atmo.infra.jpa.entity.IngestionRunEntity
import org.springframework.data.jpa.repository.JpaRepository

interface IngestionRunRepo : JpaRepository<IngestionRunEntity, Long>

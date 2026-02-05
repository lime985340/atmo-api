package lime.atmos.infra.jpa.repo

import lime.atmos.infra.jpa.entity.IngestionRunEntity
import org.springframework.data.jpa.repository.JpaRepository

interface IngestionRunRepo : JpaRepository<IngestionRunEntity, Long>

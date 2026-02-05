package lime.atmos.infra.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(
    name = "ingestion_runs",
    indexes = [Index(name = "idx_ingestion_run_job_started", columnList = "job_type,started_at DESC")],
)
class IngestionRunEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "job_type", nullable = false)
    var jobType: String,

    @Column(name = "started_at", nullable = false)
    var startedAt: Instant = Instant.now(),

    @Column(name = "ended_at")
    var endedAt: Instant? = null,

    @Column(nullable = false)
    var status: String,

    @Column(name = "last_error")
    var lastError: String? = null,

    @Column(name = "success_count", nullable = false)
    var successCount: Int = 0,

    @Column(name = "fail_count", nullable = false)
    var failCount: Int = 0,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)

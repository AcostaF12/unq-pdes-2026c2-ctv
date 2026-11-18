package unq.pdes.backend.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "demo_seed_runs")
class DemoSeedRun(
    @Id @Column(length = 60) var id: String = "ctv-demo-v1",
    @Column(name = "completed_at") var completedAt: LocalDateTime? = null,
)

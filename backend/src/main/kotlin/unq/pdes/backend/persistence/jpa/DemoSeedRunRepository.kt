package unq.pdes.backend.persistence.jpa

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import unq.pdes.backend.model.DemoSeedRun

interface DemoSeedRunRepository : JpaRepository<DemoSeedRun, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from DemoSeedRun s where s.id = 'ctv-demo-v1'")
    fun lockDemoRun(): DemoSeedRun
}

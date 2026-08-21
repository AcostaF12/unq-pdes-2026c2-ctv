package unq.pdes.backend.persistence.jpa

import org.springframework.data.jpa.repository.JpaRepository
import unq.pdes.backend.model.Agency

interface AgencyRepository : JpaRepository<Agency, Long> {

    fun findByName(name: String): Agency?

    fun existsByName(name: String): Boolean
}

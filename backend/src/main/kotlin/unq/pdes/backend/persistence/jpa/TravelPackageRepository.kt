package unq.pdes.backend.persistence.jpa

import org.springframework.data.jpa.repository.JpaRepository
import unq.pdes.backend.model.TravelPackage

interface TravelPackageRepository : JpaRepository<TravelPackage, Long> {

    fun existsByAgencyIdAndName(agencyId: Long, name: String): Boolean
}

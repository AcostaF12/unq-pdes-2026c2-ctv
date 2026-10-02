package unq.pdes.backend.persistence.jpa

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import unq.pdes.backend.model.TravelPackage

interface TravelPackageRepository :
    JpaRepository<TravelPackage, Long>,
    JpaSpecificationExecutor<TravelPackage> {
    fun existsByAgencyIdAndName(
        agencyId: Long,
        name: String,
    ): Boolean

    fun existsByAgencyIdAndNameAndIdNot(
        agencyId: Long,
        name: String,
        id: Long,
    ): Boolean

    fun findByAgencyId(agencyId: Long): List<TravelPackage>
}

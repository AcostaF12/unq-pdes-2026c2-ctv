package unq.pdes.backend.persistence.jpa

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import unq.pdes.backend.model.TravelPackage

interface TravelPackageRepository : JpaRepository<TravelPackage, Long> {

    fun existsByAgencyIdAndName(agencyId: Long, name: String): Boolean

    fun existsByAgencyIdAndNameAndIdNot(agencyId: Long, name: String, id: Long): Boolean

    fun findByAgencyId(agencyId: Long): List<TravelPackage>

    @Query(
        """
        SELECT p FROM TravelPackage p
        WHERE (:name IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', CAST(:name AS string), '%')))
          AND (:origin IS NULL OR p.origin.code = :origin)
          AND (:destination IS NULL OR p.destination.code = :destination)
        """,
    )
    fun search(
        @Param("name") name: String?,
        @Param("origin") origin: String?,
        @Param("destination") destination: String?,
    ): List<TravelPackage>
}

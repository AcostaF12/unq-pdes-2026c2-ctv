package unq.pdes.backend.persistence.jpa

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.Repository
import unq.pdes.backend.model.Purchase
import unq.pdes.backend.model.report.CountRanking
import unq.pdes.backend.model.report.DestinationCountRanking
import unq.pdes.backend.model.report.DestinationRatingRanking

interface AdminReportRepository : Repository<Purchase, Long> {
    @Query(
        """SELECT new unq.pdes.backend.model.report.CountRanking(p.buyer.id, p.buyer.username, COUNT(p))
        FROM Purchase p GROUP BY p.buyer.id, p.buyer.username
        ORDER BY COUNT(p) DESC, p.buyer.username ASC, p.buyer.id ASC""",
    )
    fun topBuyers(pageable: Pageable): List<CountRanking>

    @Query(
        """SELECT new unq.pdes.backend.model.report.CountRanking(p.agency.id, p.agency.name, COUNT(p))
        FROM Purchase p GROUP BY p.agency.id, p.agency.name
        ORDER BY COUNT(p) DESC, p.agency.name ASC, p.agency.id ASC""",
    )
    fun topAgencies(pageable: Pageable): List<CountRanking>

    @Query(
        """SELECT new unq.pdes.backend.model.report.DestinationCountRanking(
        p.travelSnapshot.destinationCode, MAX(p.travelSnapshot.destinationName), COUNT(p))
        FROM Purchase p GROUP BY p.travelSnapshot.destinationCode
        ORDER BY COUNT(p) DESC, p.travelSnapshot.destinationCode ASC""",
    )
    fun topDestinations(pageable: Pageable): List<DestinationCountRanking>

    @Query(
        """SELECT new unq.pdes.backend.model.report.DestinationRatingRanking(
        r.travelPackage.destination.code, r.travelPackage.destination.name, AVG(r.score), COUNT(r))
        FROM Review r GROUP BY r.travelPackage.destination.code, r.travelPackage.destination.name
        ORDER BY AVG(r.score) DESC, COUNT(r) DESC, r.travelPackage.destination.code ASC""",
    )
    fun topRatedDestinations(pageable: Pageable): List<DestinationRatingRanking>
}

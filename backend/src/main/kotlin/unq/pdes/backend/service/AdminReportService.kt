package unq.pdes.backend.service

import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import unq.pdes.backend.persistence.jpa.AdminReportRepository

@Service
@Transactional(readOnly = true)
class AdminReportService(
    private val repository: AdminReportRepository,
) {
    private val topFive = PageRequest.of(0, 5)

    fun topBuyers() = repository.topBuyers(topFive)

    fun topAgencies() = repository.topAgencies(topFive)

    fun topDestinations() = repository.topDestinations(topFive)

    fun topRatedDestinations() = repository.topRatedDestinations(topFive)
}

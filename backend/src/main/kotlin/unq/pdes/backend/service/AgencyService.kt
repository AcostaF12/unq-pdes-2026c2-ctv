package unq.pdes.backend.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import unq.pdes.backend.model.Agency
import unq.pdes.backend.persistence.jpa.AgencyRepository

@Service
class AgencyService(
    private val agencyRepository: AgencyRepository,
) {
    @Transactional(readOnly = true)
    fun findAll(): List<Agency> = agencyRepository.findAllByOrderByNameAsc()

    @Transactional(readOnly = true)
    fun findById(id: Long): Agency =
        agencyRepository
            .findById(id)
            .orElseThrow { EntityNotFoundException("There is no Agency with id: $id.") }
}

package unq.pdes.backend.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import unq.pdes.backend.model.Agency
import unq.pdes.backend.persistence.jpa.AgencyRepository

@Service
class AgencyService(
    private val agencyRepository: AgencyRepository,
    private val userService: UserService,
) {
    @Transactional(readOnly = true)
    fun findAll(): List<Agency> = agencyRepository.findAllByOrderByNameAsc()

    @Transactional
    fun create(
        name: String,
        username: String,
        password: String,
        firstName: String,
        lastName: String,
    ): Agency {
        val agency = agencyRepository.save(Agency.Builder().name(name.trim()).build())
        userService.createAgencyUser(username.trim(), password, firstName.trim(), lastName.trim(), agency)
        return agency
    }

    @Transactional
    fun update(
        id: Long,
        name: String,
        active: Boolean,
    ): Agency {
        require(name.isNotBlank()) { "The agency must have a name." }
        val agency = findById(id)
        agency.name = name.trim()
        agency.active = active
        return agencyRepository.save(agency)
    }

    @Transactional(readOnly = true)
    fun findById(id: Long): Agency =
        agencyRepository
            .findById(id)
            .orElseThrow { EntityNotFoundException("There is no Agency with id: $id.") }
}

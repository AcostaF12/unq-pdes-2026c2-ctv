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
        require(password.toByteArray(Charsets.UTF_8).size <= 72) { "La contraseña supera los 72 bytes." }
        val agency = agencyRepository.save(Agency.Builder().name(validatedName(name)).build())
        userService.createAgencyUser(username.trim(), password, firstName.trim(), lastName.trim(), agency)
        return agency
    }

    @Transactional
    fun update(
        id: Long,
        name: String,
        active: Boolean,
    ): Agency {
        val agency = findById(id)
        agency.name = validatedName(name, id)
        agency.active = active
        return agencyRepository.save(agency)
    }

    private fun validatedName(
        name: String,
        id: Long? = null,
    ): String {
        val normalized = name.trim()
        require(normalized.isNotBlank() && normalized.length <= 160) { "El nombre de la agencia debe tener entre 1 y 160 caracteres." }
        require(agencyRepository.findAllByNameIgnoreCase(normalized).none { it.id != id }) { "Ya existe una agencia con ese nombre." }
        return normalized
    }

    @Transactional(readOnly = true)
    fun findById(id: Long): Agency =
        agencyRepository
            .findById(id)
            .orElseThrow { EntityNotFoundException("There is no Agency with id: $id.") }
}

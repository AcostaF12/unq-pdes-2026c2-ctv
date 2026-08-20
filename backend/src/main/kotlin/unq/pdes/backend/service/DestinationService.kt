package unq.pdes.backend.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import unq.pdes.backend.model.Destination
import unq.pdes.backend.persistence.jpa.DestinationRepository

@Service
class DestinationService(
    private val destinationRepository: DestinationRepository,
) {

    @Transactional(readOnly = true)
    fun findAll(): List<Destination> {
        return destinationRepository.findAll()
    }

    @Transactional(readOnly = true)
    fun findByCode(code: String): Destination {
        return destinationRepository.findById(code)
            .orElseThrow { EntityNotFoundException("There is no Destination with code: $code.") }
    }

    @Transactional(readOnly = true)
    fun existsByCode(code: String): Boolean {
        return destinationRepository.existsById(code)
    }

    @Transactional
    fun save(destination: Destination): Destination {
        return destinationRepository.save(destination)
    }
}

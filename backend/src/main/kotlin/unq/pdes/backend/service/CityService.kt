package unq.pdes.backend.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import unq.pdes.backend.model.City
import unq.pdes.backend.persistence.jpa.CityRepository

@Service
class CityService(
    private val cityRepository: CityRepository,
) {

    @Transactional(readOnly = true)
    fun findAll(): List<City> {
        return cityRepository.findAll()
    }

    @Transactional(readOnly = true)
    fun findByCode(code: String): City {
        return cityRepository.findById(code)
            .orElseThrow { EntityNotFoundException("There is no City with code: $code.") }
    }

    @Transactional(readOnly = true)
    fun existsByCode(code: String): Boolean {
        return cityRepository.existsById(code)
    }

    @Transactional
    fun save(city: City): City {
        return cityRepository.save(city)
    }
}

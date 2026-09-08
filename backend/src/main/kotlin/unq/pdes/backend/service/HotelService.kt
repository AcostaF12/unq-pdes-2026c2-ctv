package unq.pdes.backend.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import unq.pdes.backend.model.Hotel
import unq.pdes.backend.persistence.jpa.HotelRepository

@Service
class HotelService(
    private val hotelRepository: HotelRepository,
    private val cityService: CityService,
) {

    @Transactional(readOnly = true)
    fun findAll(): List<Hotel> {
        return hotelRepository.findAll()
    }

    @Transactional(readOnly = true)
    fun findById(id: Long): Hotel {
        return hotelRepository.findById(id)
            .orElseThrow { EntityNotFoundException("There is no Hotel with id: $id.") }
    }

    @Transactional
    fun create(name: String, cityCode: String, photoUrl: String): Hotel {
        val city = cityService.findByCode(cityCode)
        val hotel = Hotel.Builder()
            .name(name)
            .city(city)
            .photoUrl(photoUrl)
            .build()
        return hotelRepository.save(hotel)
    }

    @Transactional
    fun update(id: Long, name: String, cityCode: String, photoUrl: String): Hotel {
        findById(id)
        val city = cityService.findByCode(cityCode)
        val hotel = Hotel.Builder()
            .id(id)
            .name(name)
            .city(city)
            .photoUrl(photoUrl)
            .build()
        return hotelRepository.save(hotel)
    }

    @Transactional
    fun deleteById(id: Long) {
        val hotel = findById(id)
        hotelRepository.delete(hotel)
    }

    @Transactional
    fun save(hotel: Hotel): Hotel {
        return hotelRepository.save(hotel)
    }
}

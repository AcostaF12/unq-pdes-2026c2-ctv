package unq.pdes.backend.persistence.jpa

import org.springframework.data.jpa.repository.JpaRepository
import unq.pdes.backend.model.Hotel

interface HotelRepository : JpaRepository<Hotel, Long>

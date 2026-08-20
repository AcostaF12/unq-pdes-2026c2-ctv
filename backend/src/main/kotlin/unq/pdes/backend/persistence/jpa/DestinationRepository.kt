package unq.pdes.backend.persistence.jpa

import org.springframework.data.jpa.repository.JpaRepository
import unq.pdes.backend.model.Destination

interface DestinationRepository : JpaRepository<Destination, String>

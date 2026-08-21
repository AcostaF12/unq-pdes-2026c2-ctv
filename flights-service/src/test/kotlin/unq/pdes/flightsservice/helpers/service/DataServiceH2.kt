package unq.pdes.flightsservice.helpers.service

import org.springframework.stereotype.Service
import unq.pdes.flightsservice.helpers.persistence.DataRepositoryH2

@Service
class DataServiceH2(
    private val dataRepositoryH2: DataRepositoryH2,
) {

    fun deleteAll() {
        dataRepositoryH2.deleteAll()
    }
}

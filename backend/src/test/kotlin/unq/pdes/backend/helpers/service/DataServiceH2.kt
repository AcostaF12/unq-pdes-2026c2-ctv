package unq.pdes.backend.helpers.service

import org.springframework.stereotype.Service
import unq.pdes.backend.helpers.persistence.DataRepositoryH2

@Service
class DataServiceH2(
    private val dataRepositoryH2: DataRepositoryH2,
) {

    fun deleteAll() {
        dataRepositoryH2.deleteAll()
    }
}

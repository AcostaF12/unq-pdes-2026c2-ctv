package unq.pdes.backend.tests.integration.service

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import unq.pdes.backend.helpers.factory.PersistentObjectsFactory
import unq.pdes.backend.helpers.service.DataServiceH2
import unq.pdes.backend.service.FavoriteService

@SpringBootTest
class FavoriteServiceTest {

    @Autowired
    private lateinit var favoriteService: FavoriteService

    @Autowired
    private lateinit var factory: PersistentObjectsFactory

    @Autowired
    private lateinit var dataServiceH2: DataServiceH2

    @AfterEach
    fun tearDown() {
        dataServiceH2.deleteAll()
    }

    @Test
    fun `01 - add should persist a favorite`() {
        val buyer = factory.buyerNamed("buyer")
        val travelPackage = factory.packageNamed("París Romántico")

        val favorite = favoriteService.add(buyer.username, travelPackage.id!!)

        assertEquals(travelPackage.id, favorite.travelPackage.id)
        assertEquals(1, favoriteService.findMine(buyer.username).size)
    }

    @Test
    fun `02 - adding the same package twice should fail`() {
        val buyer = factory.buyerNamed("buyer")
        val travelPackage = factory.packageNamed("París Romántico")
        favoriteService.add(buyer.username, travelPackage.id!!)

        val exception = assertThrows(IllegalArgumentException::class.java) {
            favoriteService.add(buyer.username, travelPackage.id!!)
        }

        assertEquals("The package is already in favorites.", exception.message)
    }

    @Test
    fun `03 - remove should delete the favorite`() {
        val buyer = factory.buyerNamed("buyer")
        val travelPackage = factory.packageNamed("París Romántico")
        favoriteService.add(buyer.username, travelPackage.id!!)

        favoriteService.remove(buyer.username, travelPackage.id!!)

        assertEquals(0, favoriteService.findMine(buyer.username).size)
    }

    @Test
    fun `04 - remove should fail when the package is not in favorites`() {
        val buyer = factory.buyerNamed("buyer")
        val travelPackage = factory.packageNamed("París Romántico")

        val exception = assertThrows(jakarta.persistence.EntityNotFoundException::class.java) {
            favoriteService.remove(buyer.username, travelPackage.id!!)
        }

        assertEquals("The package is not in favorites.", exception.message)
    }

    @Test
    fun `05 - agency users cannot manage favorites`() {
        factory.agencyUserNamed("agency")
        val travelPackage = factory.packageNamed("París Romántico")

        val exception = assertThrows(org.springframework.security.access.AccessDeniedException::class.java) {
            favoriteService.add("agency", travelPackage.id!!)
        }

        assertEquals("Only buyers can manage favorites.", exception.message)
    }
}

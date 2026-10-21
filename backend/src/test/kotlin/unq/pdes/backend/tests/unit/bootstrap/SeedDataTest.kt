package unq.pdes.backend.tests.unit.bootstrap

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import unq.pdes.backend.config.bootstrap.SeedData
import unq.pdes.backend.config.bootstrap.SeedFlightIds

class SeedDataTest {
    @Test
    fun `flight ids follow the convention shared with flights-service`() {
        assertEquals(1L, SeedFlightIds.outbound("PAR", 0))
        assertEquals(2L, SeedFlightIds.inbound("PAR", 0))
        assertEquals(7L, SeedFlightIds.outbound("LON", 0))
        assertEquals(54L, SeedFlightIds.inbound("DXB", 2))
    }

    @Test
    fun `flight ids reject unknown destinations and variants`() {
        assertThrows(IllegalArgumentException::class.java) { SeedFlightIds.outbound("XXX", 0) }
        assertThrows(IllegalArgumentException::class.java) { SeedFlightIds.outbound("PAR", 3) }
    }

    @Test
    fun `every package points to a seeded hotel located in its destination`() {
        val hotels = SeedData.HOTELS.associateBy { it.name }

        SeedData.PACKAGES.forEach { seed ->
            assertEquals(seed.destination, hotels.getValue(seed.hotel).cityCode, seed.name)
            assertTrue(seed.agency in SeedData.AGENCIES, seed.name)
        }
    }

    @Test
    fun `package names are unique and purchases reviews and favorites reference existing data`() {
        val packageNames = SeedData.PACKAGES.map { it.name }
        val buyers = SeedData.BUYERS.map { it.username }.toSet()
        val purchased = SeedData.PURCHASES.map { it.buyer to it.travelPackage }.toSet()

        assertEquals(packageNames.size, packageNames.toSet().size)
        assertEquals(SeedData.PURCHASES.size, purchased.size)
        assertTrue(SeedData.PURCHASES.all { it.buyer in buyers && it.travelPackage in packageNames })
        assertTrue(SeedData.REVIEWS.all { (it.buyer to it.travelPackage) in purchased && it.score in 0..10 })
        val reviewed = SeedData.REVIEWS.map { it.buyer to it.travelPackage }
        assertEquals(SeedData.REVIEWS.size, reviewed.toSet().size)
        assertTrue(SeedData.FAVORITES.all { it.buyer in buyers && it.travelPackage in packageNames })
        assertEquals(SeedData.FAVORITES.size, SeedData.FAVORITES.toSet().size)
    }

    @Test
    fun `hotel photos are real and distinct per hotel`() {
        val urls = SeedData.HOTELS.map { SeedData.photoUrl(it) }

        assertTrue(urls.all { it.startsWith("https://images.unsplash.com/photo-") })
        assertEquals(urls.size, urls.toSet().size)
    }
}

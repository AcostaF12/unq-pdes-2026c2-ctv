package unq.pdes.backend.tests.integration.controller

import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.sql.DriverManager

class AgencyMigrationTest {
    @Test
    fun `V3 keeps existing agencies active and does not run twice`() {
        val url = "jdbc:h2:mem:agency_upgrade;DB_CLOSE_DELAY=-1"
        Flyway
            .configure()
            .dataSource(url, "sa", "")
            .target("2")
            .load()
            .migrate()
        DriverManager.getConnection(url, "sa", "").use { connection ->
            connection.createStatement().use { sql ->
                sql.executeUpdate("INSERT INTO agencies(name) VALUES ('Before migration')")
                val flyway = Flyway.configure().dataSource(url, "sa", "").load()
                assertEquals(1, flyway.migrate().migrationsExecuted)
                sql.executeQuery("SELECT name, active FROM agencies").use { rows ->
                    assertTrue(rows.next())
                    assertEquals("Before migration", rows.getString("name"))
                    assertTrue(rows.getBoolean("active"))
                }
                assertEquals(0, flyway.migrate().migrationsExecuted)
            }
        }
    }
}

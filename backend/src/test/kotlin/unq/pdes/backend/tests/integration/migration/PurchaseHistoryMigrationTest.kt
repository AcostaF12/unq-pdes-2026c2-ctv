package unq.pdes.backend.tests.integration.migration

import java.sql.DriverManager
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.MigrationVersion
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PurchaseHistoryMigrationTest {

    @Test
    fun `legacy purchase is backfilled with buyer hotel trip and paid price snapshots`() {
        val url = "jdbc:h2:mem:purchase_history_migration;DB_CLOSE_DELAY=-1"
        Flyway.configure()
            .dataSource(url, "sa", "")
            .target(MigrationVersion.fromVersion("1"))
            .load()
            .migrate()

        DriverManager.getConnection(url, "sa", "").use { connection ->
            connection.createStatement().use { statement ->
                statement.executeUpdate("INSERT INTO agencies (id, name) VALUES (1, 'Agencia')")
                statement.executeUpdate("INSERT INTO cities (code, name) VALUES ('BUE', 'Buenos Aires'), ('PAR', 'París')")
                statement.executeUpdate(
                    "INSERT INTO users (id, user_type, username, password, role, first_name, last_name) " +
                        "VALUES (1, 'STANDARD', 'comprador', 'hash', 'BUYER', 'Ana', 'Cliente')",
                )
                statement.executeUpdate(
                    "INSERT INTO hotels (id, name, city_code, photo_url) VALUES (1, 'Hotel París', 'PAR', '/hotel.jpg')",
                )
                statement.executeUpdate(
                    "INSERT INTO packages (id, agency_id, hotel_id, origin_city_code, destination_city_code, name, " +
                        "outbound_flight_id, return_flight_id, price) " +
                        "VALUES (1, 1, 1, 'BUE', 'PAR', 'Escapada', 10, 11, 200.00)",
                )
                statement.executeUpdate(
                    "INSERT INTO purchases (id, buyer_id, package_id, agency_id, purchase_price, purchased_at) " +
                        "VALUES (1, 1, 1, 1, 125.00, TIMESTAMP '2025-01-02 10:30:00')",
                )
            }
        }

        Flyway.configure().dataSource(url, "sa", "").load().migrate()

        DriverManager.getConnection(url, "sa", "").use { connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery(
                    "SELECT buyer_snapshot_username, package_snapshot_name, package_snapshot_price, " +
                        "hotel_snapshot_name, origin_snapshot_name, destination_snapshot_name " +
                        "FROM purchases WHERE id = 1",
                ).use { result ->
                    result.next()
                    assertEquals("comprador", result.getString("buyer_snapshot_username"))
                    assertEquals("Escapada", result.getString("package_snapshot_name"))
                    assertEquals(125.00.toBigDecimal().setScale(2), result.getBigDecimal("package_snapshot_price"))
                    assertEquals("Hotel París", result.getString("hotel_snapshot_name"))
                    assertEquals("Buenos Aires", result.getString("origin_snapshot_name"))
                    assertEquals("París", result.getString("destination_snapshot_name"))
                }
            }
        }
    }
}

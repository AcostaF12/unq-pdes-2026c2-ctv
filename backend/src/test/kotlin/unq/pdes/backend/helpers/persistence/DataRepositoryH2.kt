package unq.pdes.backend.helpers.persistence

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class DataRepositoryH2(
    private val jdbcTemplate: JdbcTemplate,
) {
    fun deleteAll() {
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE")

        val tables: List<String> =
            jdbcTemplate
                .queryForList(
                    "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'PUBLIC'",
                    String::class.java,
                ).filterNotNull()
                .filterNot { it.equals("flyway_schema_history", ignoreCase = true) }

        tables.forEach { table ->
            jdbcTemplate.execute("TRUNCATE TABLE \"$table\"")
        }

        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE")
    }
}

package unq.pdes.backend.config.bootstrap

import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(prefix = "app.seed", name = ["enabled"], havingValue = "true")
class DataBootstrap(
    private val seeder: DemoDataSeeder,
) : ApplicationRunner {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun run(args: ApplicationArguments) {
        val created = seeder.seed()
        logger.info("Demo seed {}. Existing accounts, catalog and history were preserved.", if (created) "completed" else "already applied")
    }
}

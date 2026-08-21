package unq.pdes.backend.tests.unit.architecture

import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.library.Architectures.layeredArchitecture
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS

@TestInstance(PER_CLASS)
class ArchitectureTests {

    private lateinit var baseClasses: JavaClasses

    @BeforeAll
    fun setup() {
        baseClasses = ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("unq.pdes.backend")
    }

    @Test
    fun `01 - repository classes should end with Repository`() {
        classes().that().resideInAPackage("..persistence..")
            .should().haveSimpleNameEndingWith("Repository")
            .check(baseClasses)
    }

    @Test
    fun `02 - model dto classes should end with Dto`() {
        classes().that().resideInAPackage("..controller.dtos.models..")
            .and().haveSimpleNameNotContaining("Companion")
            .should().haveSimpleNameEndingWith("Dto")
            .check(baseClasses)
    }

    @Test
    fun `03 - service classes should end with Service`() {
        classes().that().resideInAPackage("..service..")
            .and().areNotAnonymousClasses()
            .should().haveSimpleNameEndingWith("Service")
            .check(baseClasses)
    }

    @Test
    fun `04 - controller classes should end with Controller`() {
        classes().that().resideInAPackage("..controller")
            .and().haveSimpleNameNotContaining("Companion")
            .should().haveSimpleNameEndingWith("Controller")
            .check(baseClasses)
    }

    @Test
    fun `05 - the layers must be respected without dependencies between them`() {
        layeredArchitecture().consideringAllDependencies()
            .layer("Controller").definedBy("..controller..")
            .layer("Service").definedBy("..service..")
            .layer("Persistence").definedBy("..persistence..")
            .layer("Config").definedBy("..config..")
            .whereLayer("Controller").mayNotBeAccessedByAnyLayer()
            .whereLayer("Service").mayOnlyBeAccessedByLayers("Controller", "Config")
            .whereLayer("Persistence").mayOnlyBeAccessedByLayers("Service", "Config")
            .check(baseClasses)
    }
}

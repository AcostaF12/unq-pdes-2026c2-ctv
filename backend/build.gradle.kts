plugins {
	kotlin("jvm") version "2.3.21"
	kotlin("plugin.spring") version "2.3.21"
	id("org.springframework.boot") version "4.1.0"
	id("io.spring.dependency-management") version "1.1.7"
	kotlin("plugin.jpa") version "2.3.21"
	id("org.sonarqube") version "7.3.1.8318"
	jacoco
}

group = "unq.pdes"
version = "0.0.1-SNAPSHOT"
description = "backend"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(25)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	implementation("org.springframework.boot:spring-boot-starter-zipkin")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("tools.jackson.module:jackson-module-kotlin")
	implementation("org.aspectj:aspectjweaver")
	implementation(libs.springdoc.openapi.starter.webmvc.ui)
	implementation(libs.resilience4j.spring.boot4)
	implementation(libs.jjwt.api)
	implementation(libs.logstash.logback.encoder)

	developmentOnly("org.springframework.boot:spring-boot-devtools")

	runtimeOnly("org.postgresql:postgresql")
	runtimeOnly("io.micrometer:micrometer-registry-prometheus")
	runtimeOnly(libs.jjwt.impl)
	runtimeOnly(libs.jjwt.gson)

	testImplementation("org.springframework.boot:spring-boot-starter-actuator-test")
	testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
	testImplementation("org.springframework.boot:spring-boot-starter-security-test")
	testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testImplementation(libs.archunit.junit5)

	testRuntimeOnly("com.h2database:h2")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
	}
}

allOpen {
	annotation("jakarta.persistence.Entity")
	annotation("jakarta.persistence.MappedSuperclass")
	annotation("jakarta.persistence.Embeddable")
}

tasks.withType<Test> {
	useJUnitPlatform()
	systemProperty("user.timezone", "America/Argentina/Buenos_Aires")
	finalizedBy(tasks.jacocoTestReport)
}

tasks.withType<JavaExec> {
	systemProperty("user.timezone", "America/Argentina/Buenos_Aires")
}

jacoco {
	toolVersion = "0.8.15"
}

tasks.jacocoTestReport {
	dependsOn(tasks.test)
	reports {
		xml.required.set(true)
		html.required.set(true)
	}
}

sonar {
	properties {
		property("sonar.projectKey", "acostaf12_unq-pdes-2026c2-ctv-backend")
		property("sonar.organization", "acostaf12")
		property("sonar.host.url", "https://sonarcloud.io")
		property("sonar.coverage.jacoco.xmlReportPaths", "build/reports/jacoco/test/jacocoTestReport.xml")
		property("sonar.coverage.exclusions", "**/*Application*.kt,**/config/**")
	}
}

tasks.named("sonar") {
	dependsOn(tasks.test, tasks.jacocoTestReport)
}

import com.google.common.collect.ImmutableMap

plugins {
	java
	id("org.springframework.boot") version "4.1.1"
	id("io.spring.dependency-management") version "1.1.7"
	id("org.openapi.generator") version "7.17.0"
}

group = "pl.student"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-data-mongodb")
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-security-oauth2-resource-server")
	testImplementation("org.springframework.boot:spring-boot-starter-security-test")
	compileOnly("org.projectlombok:lombok")
	annotationProcessor("org.projectlombok:lombok")
	testImplementation("org.springframework.boot:spring-boot-starter-data-mongodb-test")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testImplementation("org.springframework.boot:spring-boot-testcontainers")
	testImplementation("org.testcontainers:testcontainers-junit-jupiter")
	testImplementation("org.testcontainers:testcontainers-mongodb")
	testImplementation("com.atlassian.oai:swagger-request-validator-mockmvc:3.0.0")
	testCompileOnly("org.projectlombok:lombok")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
	testAnnotationProcessor("org.projectlombok:lombok")
	implementation("io.swagger.core.v3:swagger-annotations:2.2.30")

	implementation("org.mapstruct:mapstruct:1.6.3")
	annotationProcessor("org.mapstruct:mapstruct-processor:1.6.3")
}

sourceSets {
	main {
		java {
			srcDir("$buildDir/generated/openapi/src/main/java")
		}
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}

tasks.named("compileJava") {
	dependsOn(tasks.named("openApiGenerate"))
}

openApiGenerate {
    generatorName.set("spring")

    inputSpec.set(
        "$projectDir/src/main/resources/api-specification.yaml"
    )

    outputDir.set(
        "$buildDir/generated/openapi"
    )

    apiPackage.set("pl.student.sensors.generated.api")
    modelPackage.set("pl.student.sensors.generated.model")

    configOptions.set(
        mapOf(
            "interfaceOnly" to "true",
            "useTags" to "true",
            "skipDefaultInterface" to "true",
            "openApiNullable" to "false",
			"useSpringBoot3" to "true"
        )
    )
}
plugins {
	java
	id("org.springframework.boot") version "3.5.0"
	id("io.spring.dependency-management") version "1.1.7"
	id("com.google.cloud.tools.jib") version "3.4.0"
}

group = "com.niclauscott"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

configurations {
	compileOnly {
		extendsFrom(configurations.annotationProcessor.get())
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("net.jthink:jaudiotagger:2.2.5")

	implementation("net.coobird:thumbnailator:0.4.20")

	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.8")

	implementation("com.google.api-client:google-api-client:2.8.0")

	implementation("org.springframework.security:spring-security-crypto")
	implementation("io.jsonwebtoken:jjwt-api:0.12.6")
	runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
	runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

	implementation("software.amazon.awssdk:s3:2.20.90")
	implementation("io.minio:minio:8.5.17")

	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.flywaydb:flyway-core")
	implementation("org.flywaydb:flyway-database-postgresql")
	compileOnly("org.projectlombok:lombok")
	runtimeOnly("com.h2database:h2")
	runtimeOnly("org.postgresql:postgresql")
	annotationProcessor("org.projectlombok:lombok")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.security:spring-security-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}


jib {
	from {
		image = "eclipse-temurin:21-jre"
	}
	to {
		image = "jetdrive:latest"
	}
	container {
		ports = listOf("8081")
		jvmFlags = listOf("-Dspring.profiles.active=default")
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}


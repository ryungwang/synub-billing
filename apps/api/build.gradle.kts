plugins {
    java
    id("org.springframework.boot") version "3.3.13"
    id("io.spring.dependency-management") version "1.1.6"
}

group = "io.synub"
version = "0.1.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-mail")

    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")

    // 파일 저장(사업자등록증 등) — prod S3 어댑터. 로컬은 파일시스템(@Profile 분기). office와 동일 BOM.
    implementation(platform("software.amazon.awssdk:bom:2.28.29"))
    implementation("software.amazon.awssdk:s3")

    // SSO 토큰(JWT) 실제 서명검증 — HS256(공유키) 및 RS256/JWKS(SSO 공개키) 지원.
    implementation("com.nimbusds:nimbus-jose-jwt:9.40")

    // 분산 스케줄러 락 — 다중 인스턴스에서 자동청구 중복 실행 방지(DB 락).
    implementation("net.javacrumbs.shedlock:shedlock-spring:5.16.0")
    implementation("net.javacrumbs.shedlock:shedlock-provider-jdbc-template:5.16.0")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

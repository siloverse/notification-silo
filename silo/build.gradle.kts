plugins {
    id("io.github.siloverse.spring-boot-application")
    kotlin("plugin.jpa") version "2.4.0"
}

application {
    mainClass.set("io.github.siloverse.notification.ApplicationKt")
}

dependencies {
    implementation(project(":messages"))
    implementation(project(":ui"))
    implementation(project(":web"))

    implementation(local.siloverse.user.silo.messages)

    implementation(libs.bundles.spring.web)
    implementation(libs.spring.boot.starter.data.jpa)

    implementation(local.spring.boot.starter.flyway)
    implementation(local.bundles.siloverse.messaging)
    implementation(local.bundles.spring.security)
    implementation(local.bundles.jackson)

    runtimeOnly(local.flyway.postgresql)
    runtimeOnly(local.postgresql)

    testImplementation(local.spring.security.test)
    testImplementation(local.spring.boot.webmvc.test)

    testImplementation(local.spring.boot.testcontainers)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.rabbitmq)
}
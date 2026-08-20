plugins {
    id("io.github.siloverse.spring-boot-application")
}

application {
    mainClass.set("io.github.siloverse.notification.ApplicationKt")
}

dependencies {
    implementation(project(":messages"))
    implementation(project(":ui"))
    implementation(project(":web"))

    implementation(libs.bundles.spring.web)
    implementation(local.bundles.spring.security)

    testImplementation(local.spring.security.test)
    testImplementation(local.spring.boot.webmvc.test)
}
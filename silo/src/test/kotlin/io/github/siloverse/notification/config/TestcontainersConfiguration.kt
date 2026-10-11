package io.github.siloverse.notification.config

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.testcontainers.postgresql.PostgreSQLContainer

@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    fun postgres(): PostgreSQLContainer =
        PostgreSQLContainer("postgres:16")
            // same mechanism as production: unqualified messaging SQL resolves via currentSchema
            .withUrlParam("currentSchema", "notification_silo")
}

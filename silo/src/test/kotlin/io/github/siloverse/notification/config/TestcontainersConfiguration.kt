package io.github.siloverse.notification.config

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.springframework.test.context.DynamicPropertyRegistrar
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.rabbitmq.RabbitMQContainer

/**
 * Everything a {@code @SpringBootTest} context needs to start without the platform VM.
 * Every context test imports this: the silo's main configuration connects to Postgres
 * (Flyway) and to the broker (messaging) eagerly at startup.
 */
@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    fun postgres(): PostgreSQLContainer =
        PostgreSQLContainer("postgres:16")
            // same mechanism as production: unqualified messaging SQL resolves via currentSchema
            .withUrlParam("currentSchema", "notification_silo")

    // Boot starts container beans itself. No @ServiceConnection here: the messaging
    // library's connector reads our own messaging.rabbitmq.* keys, not Spring AMQP's,
    // so the container's coordinates are published as those properties instead.
    @Bean
    fun rabbit(): RabbitMQContainer = RabbitMQContainer("rabbitmq:4.1-management-alpine")

    @Bean
    fun rabbitProperties(rabbit: RabbitMQContainer) = DynamicPropertyRegistrar { registry ->
        registry.add("messaging.rabbitmq.host") { rabbit.host }
        registry.add("messaging.rabbitmq.port") { rabbit.amqpPort }
        registry.add("messaging.rabbitmq.username") { rabbit.adminUsername }
        registry.add("messaging.rabbitmq.password") { rabbit.adminPassword }
        registry.add("messaging.rabbitmq.vhost") { "/" }
    }
}

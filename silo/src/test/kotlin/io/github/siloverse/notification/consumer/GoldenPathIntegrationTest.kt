package io.github.siloverse.notification.consumer

import io.github.siloverse.messaging.core.naming.MessageNameRegistry
import io.github.siloverse.messaging.core.transport.EnvelopeFactory
import io.github.siloverse.messaging.core.transport.MessageTransport
import io.github.siloverse.messaging.core.transport.PayloadSerializer
import io.github.siloverse.notification.config.TestcontainersConfiguration
import io.github.siloverse.notification.persistence.NotificationRepository
import io.github.siloverse.user.event.UserRegistered
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.RabbitMQContainer
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.util.*

/**
 * The golden path, end to end through a real broker and a real database:
 * an envelope on the wire -> listener -> dedup inbox -> notification row.
 *
 * Duplicate delivery (same messageId) must have exactly-once EFFECT: the inbox
 * ledger and the notification INSERT commit in one transaction.
 */
@SpringBootTest
@Import(TestcontainersConfiguration::class)
class GoldenPathIntegrationTest {

    companion object {
        @JvmStatic
        val rabbit: RabbitMQContainer =
            RabbitMQContainer("rabbitmq:4.1-management-alpine").apply { start() }

        @JvmStatic
        @DynamicPropertySource
        fun rabbitProperties(registry: DynamicPropertyRegistry) {
            registry.add("messaging.rabbitmq.host") { rabbit.host }
            registry.add("messaging.rabbitmq.port") { rabbit.amqpPort }
            registry.add("messaging.rabbitmq.username") { rabbit.adminUsername }
            registry.add("messaging.rabbitmq.password") { rabbit.adminPassword }
            registry.add("messaging.rabbitmq.vhost") { "/" }
        }
    }

    @Autowired
    lateinit var transport: MessageTransport

    @Autowired
    lateinit var serializer: PayloadSerializer

    @Autowired
    lateinit var names: MessageNameRegistry

    @Autowired
    lateinit var repository: NotificationRepository

    @Test
    fun `event on the wire becomes exactly one notification row, even when redelivered`() {
        val event = UserRegistered(
            userId = UUID.randomUUID(),
            keycloakId = UUID.randomUUID(),
            email = "golden-path@test.local",
            displayName = "Golden Path",
            occurredAt = OffsetDateTime.now().truncatedTo(ChronoUnit.MILLIS),
        )
        val envelope = EnvelopeFactory(names, serializer).envelopeFor(event)

        // 1. first delivery -> one row
        transport.send(envelope)
        awaitRowCount(1)
        val row = repository.findByRecipientEmail(event.email).single()
        assertEquals("WELCOME", row.kind)

        // 2. duplicate delivery (SAME messageId, as a broker redelivery would be) -> still one row
        transport.send(envelope)
        Thread.sleep(2000) // give a wrong implementation the time to fail
        assertEquals(1, repository.findByRecipientEmail(event.email).size, "dedup must swallow the redelivery")

        // 3. a genuinely new message (fresh messageId) -> second row
        val second = EnvelopeFactory(names, serializer).envelopeFor(event.copy(userId = UUID.randomUUID()))
        transport.send(second)
        awaitRowCount(2)
    }

    private fun awaitRowCount(expected: Long, timeoutMillis: Long = 10_000) {
        val deadline = System.currentTimeMillis() + timeoutMillis
        while (System.currentTimeMillis() < deadline) {
            if (repository.count() == expected) return
            Thread.sleep(100)
        }
        fail<Nothing>("expected $expected notification rows within ${timeoutMillis}ms, found ${repository.count()}")
    }
}

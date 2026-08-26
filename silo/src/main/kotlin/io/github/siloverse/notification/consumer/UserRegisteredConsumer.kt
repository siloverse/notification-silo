package io.github.siloverse.notification.consumer

import io.github.siloverse.messaging.core.consumer.Consumer
import io.github.siloverse.notification.domain.NotificationEntity
import io.github.siloverse.notification.persistence.NotificationRepository
import io.github.siloverse.user.event.UserRegistered
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class UserRegisteredConsumer(
    private val repository: NotificationRepository,
) {

    private val logger = LoggerFactory.getLogger(UserRegisteredConsumer::class.java)

    /**
     * dedup = true: the notification INSERT and the inbox ledger row commit in one
     * transaction (started by the messaging library), so a redelivered event can
     * never record a second WELCOME notification.
     */
    @Consumer(id = "user-registered-notifier", dedup = true)
    fun consume(message: UserRegistered) {
        repository.save(
            NotificationEntity(
                recipientEmail = message.email,
                kind = "WELCOME",
                occurredAt = message.occurredAt,
            )
        )
        logger.info(
            "notification recorded: kind=WELCOME recipient={} keycloakId={}",
            message.email, message.keycloakId,
        )
    }
}

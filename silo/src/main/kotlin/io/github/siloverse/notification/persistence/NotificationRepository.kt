package io.github.siloverse.notification.persistence

import io.github.siloverse.notification.domain.NotificationEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface NotificationRepository : JpaRepository<NotificationEntity, UUID> {

    fun findByRecipientEmail(recipientEmail: String): List<NotificationEntity>
}

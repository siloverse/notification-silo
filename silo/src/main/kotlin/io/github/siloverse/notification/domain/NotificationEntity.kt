package io.github.siloverse.notification.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "notifications", schema = "notification_silo")
class NotificationEntity(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "recipient_email", nullable = false)
    val recipientEmail: String,

    @Column(nullable = false)
    val kind: String,

    @Column(name = "occurred_at", nullable = false)
    val occurredAt: OffsetDateTime,
) {
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now()
}

package io.github.siloverse.notification

import io.github.siloverse.messaging.core.naming.MessageNameRegistry

object NotificationSiloMessages {
    fun names(): MessageNameRegistry {
        return MessageNameRegistry.builder()
            .freeze()
    }
}
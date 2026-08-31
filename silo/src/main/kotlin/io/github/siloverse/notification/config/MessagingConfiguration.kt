package io.github.siloverse.notification.config

import com.rabbitmq.client.Connection
import io.github.siloverse.messaging.core.consumer.ConsumerRegistry
import io.github.siloverse.messaging.core.dispatch.MessageDispatcher
import io.github.siloverse.messaging.core.naming.MessageNameRegistry
import io.github.siloverse.messaging.core.transport.MessageTransport
import io.github.siloverse.messaging.core.transport.PayloadSerializer
import io.github.siloverse.messaging.rabbitmq.connection.RabbitMqConnectionSettings
import io.github.siloverse.messaging.rabbitmq.connection.RabbitMqConnector
import io.github.siloverse.messaging.rabbitmq.listener.RabbitMqMessageListener
import io.github.siloverse.messaging.rabbitmq.retry.FailedDeliveryHandler
import io.github.siloverse.messaging.rabbitmq.retry.RetryPolicy
import io.github.siloverse.messaging.rabbitmq.retry.RetrySettings
import io.github.siloverse.messaging.rabbitmq.retry.RetryingFailedDeliveryHandler
import io.github.siloverse.messaging.rabbitmq.topology.RabbitMqTopologyDeclarer
import io.github.siloverse.messaging.rabbitmq.transport.RabbitMqMessageTransport
import io.github.siloverse.messaging.spring.config.AsyncMessagingConfiguration
import io.github.siloverse.messaging.spring.config.MessagingConfiguration as MessagingLibraryConfiguration
import io.github.siloverse.messaging.spring.inbox.JdbcInbox
import io.github.siloverse.messaging.spring.listener.MessageListener
import io.github.siloverse.messaging.spring.serialization.JacksonPayloadDeserializer
import io.github.siloverse.messaging.spring.serialization.JacksonPayloadSerializer
import io.github.siloverse.messaging.spring.topology.TopologyDeclaration
import io.github.siloverse.user.UserSiloMessages
import org.springframework.boot.context.properties.bind.Binder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.core.env.Environment
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.support.TransactionTemplate
import tools.jackson.databind.ObjectMapper


@Configuration
@Import(
    AsyncMessagingConfiguration::class,
    MessagingLibraryConfiguration::class,
)
class MessagingConfiguration {

    // -- broker connection: config in, library handles the rest --------------
    @Bean
    fun rabbitMqConnectionSettings(
        environment: Environment
    ): RabbitMqConnectionSettings =
        Binder.get(environment)
            .bind(
                "messaging.rabbitmq",
                RabbitMqConnectionSettings::class.java
            ).orElseThrow {
                IllegalStateException(
                    "Missing messaging.rabbitmq configuration"
                )
            }

    @Bean(destroyMethod = "close")
    fun rabbitConnection(settings: RabbitMqConnectionSettings): Connection? {
        return RabbitMqConnector.connect(settings) // auto-recovering, library policy
    }

    @Bean
    fun messageTransport(connection: Connection): MessageTransport {
        return RabbitMqMessageTransport(connection) // publisher confirms: return = durably accepted
    }


    // -- serialization + names ----------------------------------------------
    @Bean
    fun payloadSerializer(mapper: ObjectMapper): PayloadSerializer {
        return JacksonPayloadSerializer(mapper)
    }

    @Bean
    fun messageNames(): MessageNameRegistry {
        // your own names + every silo you consume from, merged with cross-jar checks
        return MessageNameRegistry.compose(
            UserSiloMessages.names()
        )
    }

    @Bean
    fun retrySettings(environment: Environment): RetrySettings =
        Binder.get(environment)
            .bind("messaging.rabbitmq.default-policy", RetryPolicy::class.java)
            .map { RetrySettings(it) }
            .orElseGet { RetrySettings.DEFAULT }

    // -- topology: declared at startup, after the consumer registry freezes --
    @Bean
    fun rabbitTopology(
        connection: Connection,
        consumers: ConsumerRegistry,
        names: MessageNameRegistry,
        retrySettings: RetrySettings
    ): TopologyDeclaration {
        val declarer = RabbitMqTopologyDeclarer(connection)
        return TopologyDeclaration {
            declarer.declarePublisherTopology(names)
            declarer.declareConsumerTopology("notification-silo", consumers, names, retrySettings)
        }
    }

    @Bean
    fun failedDeliveryHandler(retrySettings: RetrySettings): FailedDeliveryHandler {
        return RetryingFailedDeliveryHandler(retrySettings, "notification-silo")
    }


    // -- consuming: own connection, owned by the assembly --------------------
    @Bean
    fun rabbitListener(
        settings: RabbitMqConnectionSettings,
        consumers: ConsumerRegistry,
        names: MessageNameRegistry,
        mapper: ObjectMapper,
        dispatcher: MessageDispatcher,
        jdbcTemplate: JdbcTemplate,
        transactionTemplate: TransactionTemplate,
        failedDeliveryHandler: FailedDeliveryHandler
    ): MessageListener {
        return object : MessageListener {
            private var consumeConnection: Connection? = null
            private var listener: RabbitMqMessageListener? = null

            override fun start() {
                // separate connection: broker flow control throttles publishers and
                // must not starve consumers
                consumeConnection = RabbitMqConnector.connect(settings)
                listener = RabbitMqMessageListener(
                    consumeConnection,
                    "notification-silo",
                    consumers,
                    names,
                    JacksonPayloadDeserializer(mapper),
                    dispatcher,
                    JdbcInbox(
                        jdbcTemplate,
                        transactionTemplate
                    ),
                    failedDeliveryHandler
                )
                listener!!.start()
            }

            override fun stop() {
                if (listener != null) listener!!.stop()
                try {
                    if (consumeConnection != null) consumeConnection!!.close()
                } catch (_: Exception) {
                }
            }
        }
    }
}
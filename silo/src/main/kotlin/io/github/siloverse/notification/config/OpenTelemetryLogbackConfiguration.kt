package io.github.siloverse.notification.config

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender
import org.springframework.beans.factory.InitializingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Bridges the Logback OpenTelemetryAppender (declared in logback-spring.xml) to Boot's
 * auto-configured OpenTelemetry SDK. Boot builds the SDK and the OTLP log exporter from
 * management.opentelemetry.logging.* but does NOT install the appender -- without this
 * one call the appender silently drops every event.
 */
@Configuration
class OpenTelemetryLogbackConfiguration {

    @Bean
    fun openTelemetryLogbackInstaller(openTelemetry: OpenTelemetry): InitializingBean =
        InitializingBean { OpenTelemetryAppender.install(openTelemetry) }
}

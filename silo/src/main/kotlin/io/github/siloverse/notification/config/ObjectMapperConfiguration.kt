package io.github.siloverse.notification.config

import org.springframework.context.annotation.Bean
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.cfg.DateTimeFeature
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule

class ObjectMapperConfiguration {

    @Bean
    fun objectMapper(): ObjectMapper =
        JsonMapper.builder()
            .addModule(KotlinModule.Builder().build())
            .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build()
}
package ru.seeforme.notification.service.configuration.kafka;

import lombok.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Value
@ConfigurationProperties(prefix = "spring.kafka")
public class KafkaBootstrapServersConfig {

    private String bootstrapServers;
}

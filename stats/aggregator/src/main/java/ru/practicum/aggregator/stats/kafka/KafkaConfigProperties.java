package ru.practicum.aggregator.stats.kafka;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Properties;

@Getter
@Setter
@Component
@ConfigurationProperties("aggregator.kafka")
public class KafkaConfigProperties {
    private Properties producerProps;
    private Properties consumerProps;
    private String userActionsTopic;
    private String eventsSimilarityTopic;
}

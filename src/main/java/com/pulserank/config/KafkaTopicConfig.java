package com.pulserank.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    public static final String RATING_EVENTS_TOPIC = "rating-events";

    @Bean
    public NewTopic ratingEventsTopic() {
        return TopicBuilder.name(RATING_EVENTS_TOPIC)
                .partitions(4)
                .replicas(1)
                .build();
    }
}

package com.pulserank.service;

import com.pulserank.config.KafkaTopicConfig;
import com.pulserank.event.RatingSubmittedEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class RatingEventProducer {

    private final KafkaTemplate<String, RatingSubmittedEvent> kafkaTemplate;

    public RatingEventProducer(KafkaTemplate<String, RatingSubmittedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * 用 productId 做 key：同一个商品的评分事件固定落到同一个分区，保证消费顺序。
     * 这里落库是"插入一条明细 + 整体失效缓存"，最终聚合结果和处理顺序无关，
     * 但同商品有序仍然是更安全的默认选择，为以后换成增量更新式聚合留余地。
     */
    public void publish(RatingSubmittedEvent event) {
        kafkaTemplate.send(KafkaTopicConfig.RATING_EVENTS_TOPIC, event.getProductId().toString(), event);
    }
}

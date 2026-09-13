package com.pulserank.service;

import com.pulserank.dto.RatingRequest;
import com.pulserank.event.RatingSubmittedEvent;
import org.springframework.stereotype.Service;

@Service
public class RatingService {

    private final RatingEventProducer ratingEventProducer;

    public RatingService(RatingEventProducer ratingEventProducer) {
        this.ratingEventProducer = ratingEventProducer;
    }

    /**
     * 写路径改成只发 Kafka，不再同步落库——落库和缓存失效交给 RatingEventConsumer
     * 异步完成。请求校验（@Valid）仍然是同步的，只有数据库写入被削峰。
     */
    public void submitRating(RatingRequest request) {
        ratingEventProducer.publish(
                new RatingSubmittedEvent(request.getProductId(), request.getUserId(), request.getScore()));
    }
}

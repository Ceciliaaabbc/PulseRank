package com.pulserank.service;

import com.pulserank.event.RatingSubmittedEvent;
import com.pulserank.repository.ShardedRatingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class RatingEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(RatingEventConsumer.class);

    private final ShardedRatingRepository ratingRepository;
    private final ScoreQueryService scoreQueryService;

    public RatingEventConsumer(ShardedRatingRepository ratingRepository, ScoreQueryService scoreQueryService) {
        this.ratingRepository = ratingRepository;
        this.scoreQueryService = scoreQueryService;
    }

    @KafkaListener(topics = "rating-events", groupId = "pulserank-rating-consumer")
    public void onRatingSubmitted(RatingSubmittedEvent event) {
        ratingRepository.insert(event.getProductId(), event.getUserId(), event.getScore());
        scoreQueryService.evict(event.getProductId());
        log.info("CONSUMED productId={} userId={} score={}", event.getProductId(), event.getUserId(), event.getScore());
    }
}

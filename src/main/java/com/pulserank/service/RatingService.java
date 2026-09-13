package com.pulserank.service;

import com.pulserank.dto.RatingRequest;
import com.pulserank.entity.Rating;
import com.pulserank.repository.ShardedRatingRepository;
import org.springframework.stereotype.Service;

@Service
public class RatingService {

    private final ShardedRatingRepository ratingRepository;
    private final ScoreQueryService scoreQueryService;

    public RatingService(ShardedRatingRepository ratingRepository, ScoreQueryService scoreQueryService) {
        this.ratingRepository = ratingRepository;
        this.scoreQueryService = scoreQueryService;
    }

    public Rating submitRating(RatingRequest request) {
        Rating saved = ratingRepository.insert(request.getProductId(), request.getUserId(), request.getScore());
        scoreQueryService.evict(request.getProductId());
        return saved;
    }
}

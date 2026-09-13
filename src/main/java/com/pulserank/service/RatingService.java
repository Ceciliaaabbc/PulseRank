package com.pulserank.service;

import com.pulserank.dto.ProductScoreProjection;
import com.pulserank.dto.ProductScoreResponse;
import com.pulserank.dto.RatingRequest;
import com.pulserank.entity.Rating;
import com.pulserank.repository.RatingRepository;
import org.springframework.stereotype.Service;

@Service
public class RatingService {

    private final RatingRepository ratingRepository;

    public RatingService(RatingRepository ratingRepository) {
        this.ratingRepository = ratingRepository;
    }

    public Rating submitRating(RatingRequest request) {
        Rating rating = new Rating();
        rating.setProductId(request.getProductId());
        rating.setUserId(request.getUserId());
        rating.setScore(request.getScore());
        return ratingRepository.save(rating);
    }

    public ProductScoreResponse getProductScore(Long productId) {
        ProductScoreProjection projection = ratingRepository.aggregateByProductId(productId);
        double avg = projection.getAvgScore() == null ? 0.0 : projection.getAvgScore();
        long count = projection.getRatingCount() == null ? 0L : projection.getRatingCount();
        return new ProductScoreResponse(productId, avg, count);
    }
}

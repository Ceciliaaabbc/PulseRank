package com.pulserank.controller;

import com.pulserank.dto.ProductScoreResponse;
import com.pulserank.dto.RatingAcceptedResponse;
import com.pulserank.dto.RatingRequest;
import com.pulserank.service.RatingService;
import com.pulserank.service.ScoreQueryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class RatingController {

    private final RatingService ratingService;
    private final ScoreQueryService scoreQueryService;

    public RatingController(RatingService ratingService, ScoreQueryService scoreQueryService) {
        this.ratingService = ratingService;
        this.scoreQueryService = scoreQueryService;
    }

    @PostMapping("/ratings")
    public ResponseEntity<RatingAcceptedResponse> submitRating(@Valid @RequestBody RatingRequest request) {
        ratingService.submitRating(request);
        RatingAcceptedResponse body = new RatingAcceptedResponse(
                request.getProductId(), request.getUserId(), request.getScore());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(body);
    }

    @GetMapping("/products/{productId}/score")
    public ResponseEntity<ProductScoreResponse> getProductScore(@PathVariable Long productId) {
        return ResponseEntity.ok(scoreQueryService.getProductScore(productId));
    }
}

package com.pulserank.controller;

import com.pulserank.dto.ProductScoreResponse;
import com.pulserank.dto.RatingRequest;
import com.pulserank.entity.Rating;
import com.pulserank.service.RatingService;
import jakarta.validation.Valid;
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

    public RatingController(RatingService ratingService) {
        this.ratingService = ratingService;
    }

    @PostMapping("/ratings")
    public ResponseEntity<Rating> submitRating(@Valid @RequestBody RatingRequest request) {
        return ResponseEntity.ok(ratingService.submitRating(request));
    }

    @GetMapping("/products/{productId}/score")
    public ResponseEntity<ProductScoreResponse> getProductScore(@PathVariable Long productId) {
        return ResponseEntity.ok(ratingService.getProductScore(productId));
    }
}

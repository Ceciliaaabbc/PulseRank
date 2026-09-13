package com.pulserank.dto;

public class ProductScoreResponse {

    private final Long productId;
    private final double avgScore;
    private final long ratingCount;

    public ProductScoreResponse(Long productId, double avgScore, long ratingCount) {
        this.productId = productId;
        this.avgScore = avgScore;
        this.ratingCount = ratingCount;
    }

    public Long getProductId() {
        return productId;
    }

    public double getAvgScore() {
        return avgScore;
    }

    public long getRatingCount() {
        return ratingCount;
    }
}

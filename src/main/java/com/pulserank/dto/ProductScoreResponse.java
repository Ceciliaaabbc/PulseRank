package com.pulserank.dto;

public class ProductScoreResponse {

    private final Long productId;
    private final double avgScore;
    private final long ratingCount;
    private final String source;

    public ProductScoreResponse(Long productId, double avgScore, long ratingCount, String source) {
        this.productId = productId;
        this.avgScore = avgScore;
        this.ratingCount = ratingCount;
        this.source = source;
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

    public String getSource() {
        return source;
    }
}

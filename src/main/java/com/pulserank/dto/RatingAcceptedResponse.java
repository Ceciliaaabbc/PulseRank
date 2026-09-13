package com.pulserank.dto;

public class RatingAcceptedResponse {

    private final Long productId;
    private final Long userId;
    private final Integer score;
    private final String status;

    public RatingAcceptedResponse(Long productId, Long userId, Integer score) {
        this.productId = productId;
        this.userId = userId;
        this.score = score;
        this.status = "ACCEPTED";
    }

    public Long getProductId() {
        return productId;
    }

    public Long getUserId() {
        return userId;
    }

    public Integer getScore() {
        return score;
    }

    public String getStatus() {
        return status;
    }
}

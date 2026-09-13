package com.pulserank.event;

public class RatingSubmittedEvent {

    private Long productId;
    private Long userId;
    private Integer score;

    public RatingSubmittedEvent() {
    }

    public RatingSubmittedEvent(Long productId, Long userId, Integer score) {
        this.productId = productId;
        this.userId = userId;
        this.score = score;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }
}

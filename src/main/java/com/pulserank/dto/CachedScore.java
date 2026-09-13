package com.pulserank.dto;

public class CachedScore {

    private double avgScore;
    private long ratingCount;

    public CachedScore() {
    }

    public CachedScore(double avgScore, long ratingCount) {
        this.avgScore = avgScore;
        this.ratingCount = ratingCount;
    }

    public double getAvgScore() {
        return avgScore;
    }

    public void setAvgScore(double avgScore) {
        this.avgScore = avgScore;
    }

    public long getRatingCount() {
        return ratingCount;
    }

    public void setRatingCount(long ratingCount) {
        this.ratingCount = ratingCount;
    }
}

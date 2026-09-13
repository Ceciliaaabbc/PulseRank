package com.pulserank.repository;

import com.pulserank.dto.ProductScoreProjection;
import com.pulserank.entity.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RatingRepository extends JpaRepository<Rating, Long> {

    @Query("SELECT AVG(r.score) as avgScore, COUNT(r) as ratingCount " +
           "FROM Rating r WHERE r.productId = :productId")
    ProductScoreProjection aggregateByProductId(@Param("productId") Long productId);
}

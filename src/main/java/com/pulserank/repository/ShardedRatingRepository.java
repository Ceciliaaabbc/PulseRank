package com.pulserank.repository;

import com.pulserank.dto.CachedScore;
import com.pulserank.entity.Rating;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * 手写按 product_id 取模路由到 rating_0..rating_3 四张物理表。
 * ShardingSphere-JDBC 5.4.1（Maven Central 最新可用版本）的内部元数据序列化
 * 依赖 snakeyaml 已移除的构造方法，和 Spring Boot 3.5 要求的 snakeyaml 版本
 * 硬冲突，且该版本已不支持免依赖的 Memory 模式，无法绕过。分表路由的核心
 * 逻辑（按分片键取模、落到单一分片、避免跨分片聚合）在这里手写实现同样成立。
 */
@Repository
public class ShardedRatingRepository {

    private static final int SHARD_COUNT = 4;

    private final JdbcTemplate jdbcTemplate;

    public ShardedRatingRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public static int shardOf(Long productId) {
        return Math.floorMod(productId, SHARD_COUNT);
    }

    public Rating insert(Long productId, Long userId, Integer score) {
        String table = tableFor(productId);
        LocalDateTime now = LocalDateTime.now();
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO " + table + " (product_id, user_id, score, created_at) VALUES (?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, productId);
            ps.setLong(2, userId);
            ps.setInt(3, score);
            ps.setTimestamp(4, Timestamp.valueOf(now));
            return ps;
        }, keyHolder);

        Rating rating = new Rating();
        rating.setId(keyHolder.getKey().longValue());
        rating.setProductId(productId);
        rating.setUserId(userId);
        rating.setScore(score);
        rating.setCreatedAt(now);
        return rating;
    }

    public CachedScore aggregateByProductId(Long productId) {
        String table = tableFor(productId);
        String sql = "SELECT AVG(score) avg_score, COUNT(*) rating_count FROM " + table + " WHERE product_id = ?";
        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
            double avg = rs.getObject("avg_score") == null ? 0.0 : rs.getDouble("avg_score");
            long count = rs.getLong("rating_count");
            return new CachedScore(avg, count);
        }, productId);
    }

    private String tableFor(Long productId) {
        // shardOf 的返回值只落在 0-3 这个固定的字面量集合里，不是任何外部输入字符串，
        // 拼进 SQL 不构成注入风险。
        return "rating_" + shardOf(productId);
    }
}

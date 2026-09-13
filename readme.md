# PulseRank

高并发实时评分聚合系统 —— 面向"高并发服务稳定性"方向的求职作品集项目。

以"商品评分实时聚合"为业务场景（用户提交评分 → 实时计算平均分/评分数），逐阶段引入高并发场景下的稳定性技术点：多级缓存、异步削峰、分布式锁、自研限流熔断组件、JVM 调优、全链路压测与混沌工程。每个阶段都有明确的性能基线和可复现的验证数据。

## 技术栈

- Java 21 + Spring Boot 3.5.7 + Maven，数据访问用 JdbcTemplate（见阶段1 为什么放弃 JPA）
- MySQL 8 + Redis + Kafka（Docker Compose 本地起，Kafka 用 KRaft 单节点模式，不需要 Zookeeper）
- Caffeine（L1 本地缓存）+ Redisson（L2 Redis 缓存 / 分布式锁 / 防缓存击穿）
- 手写按 `product_id` 取模的分表路由（`rating_0`～`rating_3`，为什么没用 ShardingSphere 见阶段1 文档）
- 评分写入走 Kafka 异步削峰，`POST /api/ratings` 只发消息立即返回，落库交给消费者异步完成（见阶段2 文档）
- 自研分布式流量控制组件（`com.pulserank.governance`）：Redis+Lua 实现的分布式令牌桶限流、熔断状态机、热点探测自动延长本地缓存 TTL（见阶段3 文档）

## 项目进度

- [x] 阶段0：脚手架 + 核心链路打通（详见 [docs/阶段0.md](docs/阶段0.md)）
- [x] 阶段1：多级缓存 + 分布式锁防击穿 + 分库分表（详见 [docs/阶段1.md](docs/阶段1.md)）
- [x] 阶段2：Kafka 异步削峰（详见 [docs/阶段2.md](docs/阶段2.md)）
- [x] 阶段3：自研分布式流量控制组件（详见 [docs/阶段3.md](docs/阶段3.md)）
- [ ] 阶段4：JVM 深度优化
- [ ] 阶段5：全链路压测 + 混沌工程
- [ ] 阶段6：Linux/TCP 调优 + 可观测性

## 本地运行

```bash
docker compose up -d          # 启动 MySQL + Redis + Kafka
mvn clean package -DskipTests
java -jar target/pulserank-0.0.1-SNAPSHOT.jar   # 默认端口 8081
```

## API

- `POST /api/ratings` —— 提交评分，body: `{"productId": 1001, "userId": 1, "score": 5}`（score 范围 1-5），返回 `202 Accepted`，写入通过 Kafka 异步完成
- `GET /api/products/{productId}/score` —— 查询某商品的实时平均分与评分数，响应带 `source` 字段标出命中了哪一层（`L1`/`L2`/`DB`/`DEGRADED`熔断降级），挂了分布式限流，超限返回 `429`

# PulseRank

高并发实时评分聚合系统 —— 面向"高并发服务稳定性"方向的求职作品集项目。

以"商品评分实时聚合"为业务场景（用户提交评分 → 实时计算平均分/评分数），逐阶段引入高并发场景下的稳定性技术点：多级缓存、异步削峰、分布式锁、自研限流熔断组件、JVM 调优、全链路压测与混沌工程。每个阶段都有明确的性能基线和可复现的验证数据。

## 技术栈

- Java 21 + Spring Boot 3.5.7 + Maven
- MySQL 8（Docker Compose 本地起）
- 后续阶段将引入：Redis（多级缓存）、Kafka（异步削峰）、Redisson（分布式锁）、ShardingSphere（分库分表）

## 项目进度

- [x] 阶段0：脚手架 + 核心链路打通（详见 [docs/阶段0.md](docs/阶段0.md)）
- [ ] 阶段1：多级缓存架构 + 分库分表
- [ ] 阶段2：异步削峰 + 实时聚合
- [ ] 阶段3：自研分布式流量控制组件
- [ ] 阶段4：JVM 深度优化
- [ ] 阶段5：全链路压测 + 混沌工程
- [ ] 阶段6：Linux/TCP 调优 + 可观测性

## 本地运行

```bash
docker compose up -d          # 启动 MySQL
mvn clean package -DskipTests
java -jar target/pulserank-0.0.1-SNAPSHOT.jar   # 默认端口 8081
```

## API

- `POST /api/ratings` —— 提交评分，body: `{"productId": 1001, "userId": 1, "score": 5}`（score 范围 1-5）
- `GET /api/products/{productId}/score` —— 查询某商品的实时平均分与评分数

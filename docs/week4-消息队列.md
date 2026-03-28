# Week4 高并发写实现说明

## 1. 前三周成果与当前状态

结合 `tasks/week1.png`、`tasks/week2.png`、`tasks/week3.png`，当前项目的演进脉络如下：

### Week1
- 完成秒杀系统设计文档
- 搭建 Spring Boot + MyBatis + MySQL 基础工程
- 实现用户注册、登录、按 ID 查询用户

### Week2
- 使用 Docker Compose 编排 MySQL、Redis、Nginx、双实例应用
- 完成 Nginx 负载均衡与动静分离
- 实现商品详情 Redis 缓存，并处理穿透、击穿、雪崩

### Week3
- 搭建 MySQL 主从复制环境
- 在 Spring Boot 中接入双数据源
- 通过 `@Transactional(readOnly = true)` 完成读写分离

### 当前基础
- 商品表、库存表、订单表已具备
- Redis 已经接入，可继续承担热点库存缓存
- Docker Compose 已经成为课程作业的统一运行入口
- 代码结构已具备继续扩展 `inventory / order / mq / seckill` 模块的条件

因此，Week4 最自然的做法不是重写项目，而是在现有工程上补齐“高并发写”主链路。

## 2. Week4 任务拆解

根据 `tasks/week4.png`，本周必做要求有四项：

1. 实现秒杀下单功能
2. 使用 Redis 缓存库存，Kafka 异步创建订单
3. 使用雪花算法生成订单 ID，并支持按用户 ID 或订单 ID 查询订单
4. 保证幂等性与最终一致性，避免重复下单和超卖

选做项为：

- 使用 ShardingSphere 实现订单分库分表

本次优先完成了全部必做项；分库分表保留为后续扩展方向。

## 3. 本次实现方案

### 3.1 链路总览

秒杀请求链路如下：

1. 用户调用 `POST /api/v1/seckill/orders`
2. 应用先校验用户与商品状态
3. Redis Lua 脚本执行原子校验：
   - 检查同一用户是否已抢购过该商品
   - 检查库存是否充足
   - 原子扣减 Redis 库存
   - 写入用户-商品幂等键
   - 写入订单进度 `PENDING`
4. 生成雪花订单 ID，投递 Kafka 消息
5. Kafka 消费者异步创建订单：
   - 再次校验订单是否已存在
   - 扣减 MySQL 库存
   - 插入订单表
   - 更新 Redis 订单进度为 `CREATED`
6. 如果异步建单失败：
   - Redis 自动回滚库存
   - 删除用户-商品幂等键
   - 订单进度置为 `FAILED`

### 3.2 为什么这样设计

- Redis 负责前置挡流量：高并发扣库存先在内存完成，减轻 MySQL 热点更新压力
- Kafka 负责削峰填谷：把高峰期的大量建单请求转成异步消费
- MySQL 负责最终结果：真正的订单数据与最终库存仍以数据库为准
- Redis + DB 双层幂等：Redis 防止热点重复请求，数据库唯一索引负责最终兜底

## 4. 关键实现点

### 4.1 幂等性

Redis 使用键：

- `seckill:order:user:{userId}:{productId}`

该键存在时，说明该用户已对该商品发起过有效秒杀请求，直接拒绝重复下单。

数据库中增加唯一约束：

- `uk_user_product(user_id, product_id)`

即使极端情况下 Redis 幂等键失效，数据库仍然会拒绝同一用户对同一商品插入第二条订单。

### 4.2 订单 ID

新增 `SnowflakeIdGenerator`，根据 `app.instance-id` 计算工作节点，生成全局唯一、趋势递增的订单 ID。

接口对外统一暴露为 `orderId`，数据库内部仍保存到 `orders.order_no` 字段中。

### 4.3 最终一致性

本次实现采用“Redis 预扣 + Kafka 异步建单 + 失败补偿”的简化方案：

- 成功请求先扣 Redis 库存
- Kafka 消费成功后再写 MySQL 订单与库存
- 如果消费失败，则执行 Redis 库存回滚

这样可以保证：

- Redis 热点库存不会被并发打穿
- MySQL 最终库存不会超卖
- 失败订单不会长期占用抢购资格

### 4.4 订单查询

支持两种查询方式：

- `GET /api/v1/orders/{orderId}`
- `GET /api/v1/orders?userId=...`

其中按订单 ID 查询时，如果 MySQL 订单尚未落库，会回退到 Redis 订单进度，返回 `PENDING` 或 `FAILED` 状态。

## 5. 关键文件

- `src/main/java/com/whu/distributed/seckill/common/SnowflakeIdGenerator.java`
  - 雪花算法订单 ID
- `src/main/java/com/whu/distributed/seckill/seckill/service/SeckillCacheService.java`
  - Redis 库存预扣、幂等键、失败补偿、订单进度
- `src/main/java/com/whu/distributed/seckill/seckill/service/SeckillService.java`
  - 秒杀入口服务
- `src/main/java/com/whu/distributed/seckill/mq/OrderMessageProducer.java`
  - Kafka 生产者
- `src/main/java/com/whu/distributed/seckill/mq/OrderMessageConsumer.java`
  - Kafka 消费者与异步建单
- `src/main/java/com/whu/distributed/seckill/order/service/OrderService.java`
  - 订单创建与查询
- `src/main/java/com/whu/distributed/seckill/inventory/service/InventoryService.java`
  - 库存查询
- `docker-compose.yml`
  - 新增 Kafka 容器
- `src/main/resources/application.yml`
  - 新增 Kafka 与 week4 运行配置
- `sql/init.sql`
  - 新增订单唯一约束与秒杀测试商品

## 6. 选做项说明

题目中“分库分表”明确标注为选做。

本次提交优先把必做项中的秒杀写链路、消息队列、幂等、最终一致性全部闭环完成；ShardingSphere 分库分表暂未接入，后续可以基于现有订单模块继续扩展。

# Week5 分布式事务实现说明

## 1. 本周目标

根据 `tasks/week5.png`，Week5 的核心要求是：

1. 在秒杀下单链路中保证“订单创建 + 库存扣减”一致性
2. 在支付链路中保证“订单支付 + 订单状态更新”一致性
3. 优先采用基于消息的最终一致性方案，或使用 TCC

结合当前项目已经完成的 Week4 能力，最合适的延伸方案是：

- 保留 `Redis 预扣 + Kafka 异步下单`
- 在数据库层引入“锁定库存”状态
- 新增支付消息链路，让支付结果异步驱动订单状态流转

## 2. 现状分析

前四周已经具备：

- Week1：基础 Spring Boot + MyBatis + MySQL 工程
- Week2：Redis、Nginx、Docker Compose、高并发读优化
- Week3：主从复制与读写分离
- Week4：Redis 库存预扣、Kafka 异步建单、订单进度查询

因此 Week5 不需要推翻重来，而是在现有代码上继续补全“事务状态机”。

## 3. 本次实现方案

### 3.1 下单事务：订单创建 + 库存扣减

下单链路现在分成两层：

1. **Redis 预扣阶段**
   - Lua 脚本原子校验库存和用户幂等
   - 快速挡住热点并发

2. **数据库落单阶段**
   - Kafka 消费 `order-create` 消息
   - 在一个本地事务中：
     - `inventories.available_stock -= quantity`
     - `inventories.locked_stock += quantity`
     - 插入订单，状态置为 `CREATED`

这样库存不会在“下单未支付”阶段直接被最终扣减，而是先进入锁定态，更符合分布式事务里的“资源预留”思想。

### 3.2 支付事务：支付结果 + 订单状态更新

新增支付消息链路：

1. 用户调用 `POST /api/v1/payments`
2. 应用发送 `payment` Kafka 消息
3. 消费端根据支付结果异步处理：

- **支付成功**
  - `orders.status -> PAID`
  - `inventories.locked_stock -= quantity`

- **支付失败**
  - `orders.status -> PAY_FAILED`
  - `inventories.locked_stock -= quantity`
  - `inventories.available_stock += quantity`
  - 删除 Redis 用户幂等键，允许重新抢购

## 4. 一致性说明

### 4.1 为什么说下单与库存是一致的

数据库落单阶段把“锁库存”和“插订单”放在同一个本地事务里执行：

- 要么都成功
- 要么都失败并回滚

如果 Kafka 消费失败，则 Redis 会执行补偿，把预扣库存还回去。

### 4.2 为什么说支付与订单状态是一致的

支付消息消费端把“库存确认/释放”和“订单状态更新”放在同一个本地事务中处理：

- 支付成功：库存确认和订单改为 `PAID` 一起成功
- 支付失败：库存释放和订单改为 `PAY_FAILED` 一起成功

这是一种“消息驱动 + 本地事务”的最终一致性方案。

## 5. 状态流转

订单状态流转如下：

- `PENDING`：请求已进入队列，尚未落库
- `CREATED`：订单已创建，库存已锁定，等待支付
- `PAID`：支付成功，库存正式扣减完成
- `PAY_FAILED`：支付失败，库存已释放
- `FAILED`：异步建单失败，Redis 已补偿

## 6. 关键改动文件

- `src/main/java/com/whu/distributed/seckill/inventory/mapper/InventoryMapper.java`
  - 新增库存锁定、确认、释放 SQL
- `src/main/java/com/whu/distributed/seckill/order/service/OrderService.java`
  - 新增支付成功/失败事务处理
- `src/main/java/com/whu/distributed/seckill/mq/PaymentMessageProducer.java`
  - 支付消息生产者
- `src/main/java/com/whu/distributed/seckill/mq/PaymentMessageConsumer.java`
  - 支付消息消费者
- `src/main/java/com/whu/distributed/seckill/payment/`
  - 支付 API 与请求/响应模型

## 7. 验证建议

### 7.1 下单后查看库存

下单成功但尚未支付时，期望看到：

- `availableStock` 减少
- `lockedStock` 增加
- 订单状态为 `CREATED`

### 7.2 支付成功后查看库存

支付成功后，期望看到：

- 订单状态变为 `PAID`
- `lockedStock` 下降
- `availableStock` 不回升

### 7.3 支付失败后查看库存

支付失败后，期望看到：

- 订单状态变为 `PAY_FAILED`
- `lockedStock` 下降
- `availableStock` 回升
- 同一用户可以重新发起该商品秒杀

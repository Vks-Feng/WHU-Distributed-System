# Week4 测试与验证说明

## 1. 已在当前环境完成的验证

### 1.1 Maven 编译

执行命令：

```bash
mvn -q -DskipTests compile
```

结果：

- 编译通过，无 Java 编译错误

### 1.2 雪花 ID 单元测试

执行命令：

```bash
mvn -q -Dtest=SnowflakeIdGeneratorTests test
```

结果：

- 测试通过
- 验证了连续生成 1000 个订单 ID 时不重复且严格递增

### 1.3 Maven 测试集

执行命令：

```bash
mvn -q test
```

结果：

- 测试通过
- 默认只执行不依赖外部中间件的测试
- `SecKillApplicationTests` 被标记为集成环境测试，避免在未启动 MySQL / Redis / Kafka 时误报失败

### 1.4 Docker Compose 编排校验

执行命令：

```bash
docker compose config
```

结果：

- `docker-compose.yml` 语法校验通过
- MySQL 主从、Redis、Kafka、双实例应用、Nginx 的依赖关系均被正确解析

## 2. 建议的联调验证步骤

由于当前工作主要集中在代码实现与编排补齐，完整的 Kafka + Redis + MySQL 联调建议在本地容器环境中按以下步骤执行。

### 2.1 启动所有服务

```bash
docker compose up --build -d
```

### 2.2 注册一个测试用户

```bash
curl -X POST http://localhost:8081/api/v1/users/register \
  -H "Content-Type: application/json" \
  -d '{"username":"week4_user_001","password":"123456","phone":"13800002222"}'
```

### 2.3 查看秒杀商品库存

```bash
curl http://localhost:8081/api/v1/inventories/4
```

期望现象：

- 返回商品 `4`
- 数据库库存初始值为 `5`
- Redis 库存会在首次秒杀或查询后被加载

### 2.4 发起秒杀请求

```bash
curl -X POST http://localhost:8081/api/v1/seckill/orders \
  -H "Content-Type: application/json" \
  -d '{"userId":1,"productId":4,"quantity":1}'
```

期望结果：

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "orderId": "xxxxxxxxxxxxxxxx",
    "status": "PENDING",
    "message": "request accepted"
  }
}
```

说明：

- 请求先返回 `PENDING`
- 订单随后由 Kafka 消费者异步落库

### 2.5 按订单 ID 查询

```bash
curl http://localhost:8081/api/v1/orders/{orderId}
```

期望现象：

- 刚提交时可能返回 `PENDING`
- Kafka 消费完成后返回 `CREATED`
- 响应中可看到 `userId`、`productId`、`quantity`、`amount`

### 2.6 按用户 ID 查询订单

```bash
curl "http://localhost:8081/api/v1/orders?userId=1"
```

期望现象：

- 返回该用户已成功创建的订单列表

## 3. 幂等验证

同一用户重复抢同一商品：

```bash
curl -X POST http://localhost:8081/api/v1/seckill/orders \
  -H "Content-Type: application/json" \
  -d '{"userId":1,"productId":4,"quantity":1}'
```

再次执行相同请求。

期望现象：

- 第二次请求返回失败
- 错误信息中包含 `duplicate seckill request`
- 数据库中不会出现第二条相同 `user_id + product_id` 订单

## 4. 超卖验证

可用 JMeter 或 shell 并发压测商品 `4`。

期望现象：

- 最终成功订单数不超过库存总量 `5`
- `inventories.available_stock` 不会出现负数
- Redis 预扣库存与 MySQL 最终库存保持一致

## 5. 一致性验证

查询数据库：

```bash
docker exec seckill-mysql-master mysql -uroot -proot -e "SELECT order_no, user_id, product_id, status FROM seckill.orders ORDER BY id DESC LIMIT 10;"
docker exec seckill-mysql-master mysql -uroot -proot -e "SELECT product_id, total_stock, available_stock FROM seckill.inventories WHERE product_id=4;"
```

期望现象：

- 每个成功秒杀请求都对应一条完整订单记录
- 成功订单数量与库存扣减数量一致
- 不存在重复用户订单，不存在超卖

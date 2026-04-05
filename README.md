# sec-kill

分布式系统课程作业：商品库存与秒杀系统设计与基础实现。

## 已完成内容
- 系统设计文档：[docs/system-design.md](docs/system-design.md)
  - 系统架构草图（用户/商品/订单/库存服务）
  - RESTful API 接口定义
  - 数据库 ER 图
  - 技术栈选型说明
- 基础项目框架：Spring Boot 3 + MyBatis + MySQL
- 用户功能：注册、登录、按 ID 查询用户
- Week2 高并发读能力：
  - Docker + Docker Compose 容器化部署（MySQL、Redis、后端双实例、Nginx）
  - Nginx 负载均衡（可切换算法）
  - Nginx 动静分离（静态页面 + `/api` 代理）
  - Redis 商品详情缓存（穿透/击穿/雪崩防护）
- Week3 读写分离：
  - MySQL 主从复制环境（master/slave）
  - Spring Boot 双数据源配置（写主库、读从库）
  - 基于 `@Transactional(readOnly = true)` 的读写路由
  - 读写路由验证接口与复制状态验证文档
- Week4 高并发写能力：
  - Redis 库存预扣减，防止热点商品超卖
  - Kafka 异步下单，削峰填谷
  - 雪花算法生成订单 ID
  - 同一用户同一商品只能秒杀一次
  - 支持按订单 ID、按用户 ID 查询订单
  - 秒杀进度写入 Redis，支持 `PENDING / CREATED / FAILED` 状态查询
- Week5 分布式事务：
  - 下单时数据库库存改为“预留锁定”(`available_stock -> locked_stock`)
  - 支付成功后确认扣减锁定库存，并更新订单状态为 `PAID`
  - 支付失败时释放锁定库存，并更新订单状态为 `PAY_FAILED`
  - 基于 Kafka 消息实现“下单 + 库存扣减”“支付 + 订单状态更新”的最终一致性

## 环境要求
- JDK 17+
- Maven 3.9+
- MySQL 8.0+
- Docker Desktop（用于 week2 / week3）

## 初始化数据库
1. 启动 MySQL。
2. 执行脚本：`sql/init.sql`
3. 修改配置文件中的数据库账号密码：`src/main/resources/application.yml`

## 启动项目
```bash
mvn spring-boot:run
```

默认端口：`8080`

## 接口示例
### 1. 注册
```bash
curl -X POST http://localhost:8080/api/v1/users/register \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","password":"123456","phone":"13800000000"}'
```

### 2. 登录
```bash
curl -X POST http://localhost:8080/api/v1/users/login \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","password":"123456"}'
```

### 3. 查询用户
```bash
curl http://localhost:8080/api/v1/users/1
```

### 4. 查询商品详情（带 Redis 缓存）
```bash
curl http://localhost:8080/api/v1/products/1
```

### 5. 分页查询商品
```bash
curl "http://localhost:8080/api/v1/products?page=1&size=10"
```

## Week2 / Week3 / Week4 一键启动（推荐）
在项目根目录执行：

```bash
docker compose up --build -d
```

启动后端口：
- Nginx 入口：`http://localhost:80`
- 后端实例1：`http://localhost:8081`
- 后端实例2：`http://localhost:8082`
- MySQL 主库：`localhost:3307`
- MySQL 从库：`localhost:3308`
- Redis：`localhost:6379`
- Kafka：`localhost:9092`

验证负载均衡：
```bash
curl http://localhost/api/v1/products/1
docker logs seckill-app1 --tail 20
docker logs seckill-app2 --tail 20
```

可通过 `docker-compose.yml` 中 `NGINX_LB_POLICY` 切换算法：
- `""`（空字符串）: round robin（默认轮询）
- `"least_conn;"`: 最少连接
- `"ip_hash;"`: IP 哈希

## 动静分离验证
- 访问 `http://localhost/`：静态 `index.html/css/js` 由 Nginx 直接返回
- 页面调用 `/api/v1/products/{id}`：动态请求转发给后端集群

可用浏览器开发者工具或命令观察：
```bash
curl -I http://localhost/style.css
curl http://localhost/api/v1/products/1
```

## Redis 缓存策略说明
- 缓存穿透：对不存在商品写入短 TTL 空值（`__NULL__`）
- 缓存击穿：热点 Key 查询时使用 Redis 互斥锁重建缓存
- 缓存雪崩：商品缓存 TTL 加随机抖动，避免同一时刻集中失效

相关配置在 `application.yml`：
- `cache.product.ttl-seconds`
- `cache.product.null-ttl-seconds`
- `cache.product.ttl-jitter-seconds`
- `cache.product.lock-ttl-seconds`

## Week3 读写分离验证
应用通过双数据源完成路由：
- 写请求默认走主库
- 标注 `@Transactional(readOnly = true)` 的查询走从库
- 商品查询等可容忍短暂延迟的读请求走从库
- 用户注册等一致性要求更高的写链路继续走主库

验证接口：
```bash
curl http://localhost:8081/api/v1/system/db-route/write
curl http://localhost:8081/api/v1/system/db-route/read
```

期望现象：
- `/write` 返回 `serverId = 1`、`readOnly = 0`
- `/read` 返回 `serverId = 2`、`readOnly = 1`

主从复制验证：
```bash
curl -X POST http://localhost:8081/api/v1/users/register \
  -H "Content-Type: application/json" \
  -d '{"username":"week3_demo","password":"123456","phone":"13800001111"}'

docker exec seckill-mysql-master mysql -uroot -proot -e "SELECT id, username FROM seckill.users WHERE username='week3_demo';"
docker exec seckill-mysql-slave mysql -uroot -proot -e "SELECT id, username FROM seckill.users WHERE username='week3_demo';"
docker exec seckill-mysql-slave sh -lc "printf 'SHOW REPLICA STATUS\\G\n' | mysql -uroot -proot"
```

## Week4 秒杀下单验证

### 1. 注册用户
```bash
curl -X POST http://localhost:8081/api/v1/users/register \
  -H "Content-Type: application/json" \
  -d '{"username":"week4_demo","password":"123456","phone":"13800002222"}'
```

### 2. 查看库存
```bash
curl http://localhost:8081/api/v1/inventories/4
```

### 3. 发起秒杀请求
```bash
curl -X POST http://localhost:8081/api/v1/seckill/orders \
  -H "Content-Type: application/json" \
  -d '{"userId":1,"productId":4,"quantity":1}'
```

返回示例：
```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "orderId": "287483902004183040",
    "status": "PENDING",
    "message": "request accepted"
  }
}
```

### 4. 按订单 ID 查询
```bash
curl http://localhost:8081/api/v1/orders/287483902004183040
```

### 5. 按用户 ID 查询
```bash
curl "http://localhost:8081/api/v1/orders?userId=1"
```

### 6. 幂等与超卖验证
- 同一用户重复请求同一商品，接口会返回 `duplicate seckill request`
- Redis 先扣减库存，Kafka 消费后再落库；若异步建单失败，会自动回滚 Redis 库存
- 数据库层还有 `uk_user_product(user_id, product_id)` 兜底，避免重复下单

## Week5 分布式事务验证

### 1. 发起秒杀请求
```bash
curl -X POST http://localhost:8081/api/v1/seckill/orders \
  -H "Content-Type: application/json" \
  -d '{"userId":1,"productId":4,"quantity":1}'
```

### 2. 查询订单状态
```bash
curl http://localhost:8081/api/v1/orders/{orderId}
```

在支付前，期望看到订单状态为 `CREATED`，库存表现为 `available_stock` 减少、`locked_stock` 增加。

### 3. 模拟支付成功
```bash
curl -X POST http://localhost:8081/api/v1/payments \
  -H "Content-Type: application/json" \
  -d '{"orderId":"287483902004183040","success":true}'
```

支付成功后：
- 订单状态更新为 `PAID`
- `locked_stock` 扣减回 0
- Redis 进度更新为 `PAID`

### 4. 模拟支付失败
```bash
curl -X POST http://localhost:8081/api/v1/payments \
  -H "Content-Type: application/json" \
  -d '{"orderId":"287483902004183040","success":false}'
```

支付失败后：
- 订单状态更新为 `PAY_FAILED`
- 库存从 `locked_stock` 释放回 `available_stock`
- 用户可重新发起同商品秒杀请求

## 目录结构
```text
sec-kill
├─ docs/
│  ├─ system-design.md
│  ├─ week3-读写分离.md
│  ├─ week3-测试结果.md
│  ├─ week4-消息队列.md
│  └─ week4-测试结果.md
│  └─ week5-分布式事务.md
├─ mysql/
│  └─ replica/
│     └─ init-replica.sh
├─ nginx/
│  ├─ conf/default.conf.template
│  └─ html/
├─ sql/
│  ├─ init.sql
│  └─ replication-users.sql
├─ src/main/java/com/whu/distributed/seckill/
│  ├─ common/
│  ├─ config/
│  ├─ inventory/
│  ├─ mq/
│  ├─ order/
│  ├─ payment/
│  ├─ product/
│  ├─ seckill/
│  ├─ system/
│  └─ user/
├─ src/main/resources/
│  └─ application.yml
├─ Dockerfile
├─ docker-compose.yml
└─ pom.xml
```

## 后续可扩展方向
- 增加 JWT 鉴权与接口权限控制
- 接入 ElasticSearch 实现商品搜索（week3 可选项）
- 接入 ShardingSphere 实现订单分库分表（week4 选做）
- 将单体拆分为独立微服务（user/product/order/inventory）

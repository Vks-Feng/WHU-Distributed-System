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

## 环境要求
- JDK 17+
- Maven 3.9+
- MySQL 8.0+
- Docker Desktop（用于 week2）

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

## Week2 一键启动（推荐）
在项目根目录执行：

```bash
docker compose up --build -d
```

启动后端口：
- Nginx 入口：`http://localhost:80`
- 后端实例1：`http://localhost:8081`
- 后端实例2：`http://localhost:8082`
- MySQL：`localhost:3306`
- Redis：`localhost:6379`

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

## 目录结构
```text
sec-kill
├─ docs/
│  └─ system-design.md
├─ nginx/
│  ├─ conf/default.conf.template
│  └─ html/
├─ sql/
│  └─ init.sql
├─ src/main/java/com/whu/distributed/seckill/
│  ├─ common/
│  ├─ config/
│  ├─ product/
│  └─ user/
├─ src/main/resources/
│  └─ application.yml
├─ Dockerfile
├─ docker-compose.yml
└─ pom.xml
```

## 后续可扩展方向
- 接入 Redis 预减库存与库存回滚
- 引入消息队列（RabbitMQ/Kafka）实现下单异步化
- 增加 JWT 鉴权与接口权限控制
- 将单体拆分为独立微服务（user/product/order/inventory）

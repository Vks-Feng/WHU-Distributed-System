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

## 环境要求
- JDK 17+
- Maven 3.9+
- MySQL 8.0+

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

## 目录结构
```text
sec-kill
├─ docs/
│  └─ system-design.md
├─ sql/
│  └─ init.sql
├─ src/main/java/com/whu/distributed/seckill/
│  ├─ common/
│  ├─ config/
│  └─ user/
├─ src/main/resources/
│  └─ application.yml
└─ pom.xml
```

## 后续可扩展方向
- 接入 Redis 预减库存与库存回滚
- 引入消息队列（RabbitMQ/Kafka）实现下单异步化
- 增加 JWT 鉴权与接口权限控制
- 将单体拆分为独立微服务（user/product/order/inventory）

# Week3 读写分离实现说明

## 1. 本周任务理解

根据 `tasks/week3.png`，本周高并发读任务新增的核心要求有两项：

1. 搭建 MySQL 读写分离环境，并在代码中验证读写分离效果。
2. 搭建 ElasticSearch 实现商品搜索功能（可选）。

前两周已经完成了 Docker、Nginx、JMeter 和 Redis 缓存，因此本周主线聚焦在 MySQL 主从复制与应用侧读写路由。

## 2. 本次完成内容

### 2.1 容器层
- 将原来的单 MySQL 容器拆分为 `mysql-master` 与 `mysql-slave`
- 新增 `mysql-replica-init` 初始化容器，自动执行主从复制配置
- 主库保留初始化脚本 `sql/init.sql`
- 新增 `sql/replication-users.sql`，初始化写账号、读账号、复制账号

### 2.2 应用层
- 新增双数据源配置：
  - `app.datasource.write` 指向主库
  - `app.datasource.read` 指向从库
- 使用 `LazyConnectionDataSourceProxy` 做连接延迟获取
- 对标注 `@Transactional(readOnly = true)` 的方法自动走从库
- 普通事务与默认请求走主库

### 2.3 验证辅助
- 新增接口：
  - `GET /api/v1/system/db-route/write`
  - `GET /api/v1/system/db-route/read`
- 接口会返回当前命中的数据库节点信息：
  - `hostname`
  - `serverId`
  - `readOnly`
  - `currentUser`

## 3. 设计说明

### 3.1 为什么选择 `@Transactional(readOnly = true)` 路由

这种方式改动范围小、可读性高，适合当前课程作业阶段：

- 商品查询类接口天然属于读请求，直接标注只读事务即可
- 写请求默认落主库，不需要额外注解
- 可以避免把路由逻辑散落在 Controller 或 Mapper 层

### 3.2 一致性处理策略

并不是所有查询都适合立即切到从库。

本次实现中采用了“按业务一致性要求区分”的方式：

- 商品查询 `ProductService#getById`、`ProductService#list` 走从库
- 用户注册 `UserService#register` 继续走主库
- 用户登录与用户信息读取保留在主库，避免注册后立刻读取受到复制延迟影响

这个策略更贴近真实系统中的做法：不是“所有读都去从库”，而是“适合的读流量才去从库”。

## 4. 关键文件

- `docker-compose.yml`
  - MySQL 主从与复制初始化容器编排
- `sql/replication-users.sql`
  - 初始化 `seckill_writer`、`seckill_reader`、`repl`
- `src/main/resources/application.yml`
  - 双数据源配置
- `src/main/java/com/whu/distributed/seckill/config/DataSourceConfig.java`
  - 主从数据源与路由代理配置
- `src/main/java/com/whu/distributed/seckill/config/NamedDataSource.java`
  - 记录当前获取的是读连接还是写连接
- `src/main/java/com/whu/distributed/seckill/system/*`
  - 数据库路由验证接口

## 5. 本周可选项说明

题目中的 ElasticSearch 搜索功能标记为“可选”。

本次优先完成了必做项中的 MySQL 读写分离，并把主从复制、应用路由和验证链路全部跑通。后续如需继续扩展，可以在此基础上追加商品搜索服务。

# Week3 测试结果

## 1. 读写路由接口验证

### 写路由

请求：

```bash
curl http://localhost:8081/api/v1/system/db-route/write
```

结果：

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "hostname": "99bb816e7307",
    "serverId": 1,
    "readOnly": 0,
    "databaseName": "seckill",
    "currentUser": "seckill_writer@%"
  }
}
```

说明：写路由命中了主库，`serverId=1`，且连接用户为写账号。

### 读路由

请求：

```bash
curl http://localhost:8081/api/v1/system/db-route/read
```

结果：

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "hostname": "82529c44ce07",
    "serverId": 2,
    "readOnly": 1,
    "databaseName": "seckill",
    "currentUser": "seckill_reader@%"
  }
}
```

说明：读路由命中了从库，`serverId=2`，且连接用户为只读账号。

## 2. 主从复制状态验证

执行命令：

```bash
docker exec seckill-mysql-slave sh -lc "printf 'SHOW REPLICA STATUS\\G\n' | mysql -uroot -proot"
```

关键结果：

```text
Replica_IO_Running: Yes
Replica_SQL_Running: Yes
Seconds_Behind_Source: 0
Source_Server_Id: 1
Auto_Position: 1
```

说明：从库 IO 线程与 SQL 线程均正常运行，GTID 自动定位生效，复制延迟为 0。

## 3. 写入同步验证

### 注册新用户

请求：

```bash
curl -X POST http://localhost:8081/api/v1/users/register \
  -H "Content-Type: application/json" \
  -d '{"username":"week3_user_001","password":"123456","phone":"13800001111"}'
```

结果：

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "id": 1,
    "username": "week3_user_001",
    "token": "201910cc2886433d8b8a198e95933c77"
  }
}
```

### 主库查询

```bash
docker exec seckill-mysql-master mysql -N -B -uroot -proot -e "SELECT id, username, phone FROM seckill.users WHERE username='week3_user_001';"
```

结果：

```text
1    week3_user_001    13800001111
```

### 从库查询

```bash
docker exec seckill-mysql-slave mysql -N -B -uroot -proot -e "SELECT id, username, phone FROM seckill.users WHERE username='week3_user_001';"
```

结果：

```text
1    week3_user_001    13800001111
```

说明：注册请求先写入主库，随后从库同步到了同一条数据，验证了复制链路有效。

## 4. 应用日志验证

`app1` 日志中可以看到不同请求使用了不同数据源：

```text
Acquire write datasource connection
Acquire read datasource connection
```

同时：

- `DbNodeMapper.currentNode` 的 `/write` 请求走了写连接
- `DbNodeMapper.currentNode` 的 `/read` 请求走了读连接
- `ProductMapper.findById` 查询商品时走了读连接
- `UserMapper.insert` 注册用户时走了写连接

说明：应用侧的读写路由与数据库主从复制已经形成闭环。

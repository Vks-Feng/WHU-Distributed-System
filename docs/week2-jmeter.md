# Week2 JMeter 压测建议

## 1. 负载均衡压测（动态接口）
- 目标接口：`GET http://localhost/api/v1/products/1`
- 线程组建议：
  - 线程数：100
  - Ramp-up：10s
  - 循环次数：50
- 观察点：
  - 平均响应时间、P95
  - 吞吐量（TPS）
  - `seckill-app1` 与 `seckill-app2` 日志请求量是否大致接近

## 2. 动静分离压测（静态资源）
- 静态资源：`GET http://localhost/style.css`
- 动态接口：`GET http://localhost/api/v1/products/1`
- 分别压测并对比：
  - 静态资源通常响应更快、吞吐更高
  - 动态接口受后端和数据库/缓存影响更明显

## 3. 缓存效果验证
1. 先压测 `GET /api/v1/products/1`（热 Key）。
2. 再查看 Redis 命中情况：
   - 可执行 `redis-cli monitor`（短时观察）或 `INFO stats`。
3. 请求不存在商品 `GET /api/v1/products/999999`，验证空值缓存是否生效（持续请求不应持续打满数据库）。

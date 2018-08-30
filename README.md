# 高并发商品秒杀系统
使用 Spring Boot+Mybatis+Redis+RabbitMQ 实现
- 根据商品id生成MD5加密URL，秒杀开始后暴露URL，防止提前获取URL
- 使用 Redis 将商品数量缓存，防止商品超卖
- 把用户订单信息存进 RabbitMQ，实现订单信息异步插入数据库

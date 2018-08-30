package com.ins.seckill.dao.cache;

import com.dyuproject.protostuff.runtime.RuntimeSchema;
import com.ins.seckill.entity.Seckill;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.concurrent.TimeUnit;

/**
 * The type Redis dao.
 *
 * @author blue
 */
@Component
public class RedisDao {
    /**
     * 设置 key 值的前缀
     */
    private static final String KEY_PREFIX = "seckill";
    /**
     * log4j 对象
     */
    Logger logger = LogManager.getLogger(this.getClass());

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private RuntimeSchema<Seckill> schema = RuntimeSchema.createFrom(Seckill.class);


    /**
     * 获取一个秒杀对象
     *
     * @param seckillId 秒杀商品id
     * @return 秒杀对象 seckill
     */
    public Seckill getSeckill(long seckillId) {

        String key = setPrefix(seckillId);
        Seckill seckill = (Seckill) redisTemplate.opsForValue().get(key.getBytes());
        //从缓存中重新获取到对象
        if (seckill != null) {
            return seckill;
        }

        return null;
    }

    /**
     * 设置一个秒杀对象
     *
     * @param seckill 秒杀对象
     */
    public void putSeckill(Seckill seckill) {

        String key = setPrefix(seckill.getSeckillId());
        // 设置缓存超时时间
        int timeout = 60 * 60;
        redisTemplate.opsForValue().set(key.getBytes(), seckill, timeout, TimeUnit.SECONDS);

    }

    /**
     * 设置购买记录
     * @param record
     */
    public void putSeckillRecord(String record) {
        stringRedisTemplate.opsForValue().set(record,"true");
    }

    public boolean existSeckillRecord(String record) {
        String result = stringRedisTemplate.opsForValue().get(record);
        if (StringUtils.isEmpty(result)) {
            return false;
        }
        return true;
    }

    private String setPrefix(long key) {
        String result = KEY_PREFIX + "_" + key;
        return result;
    }
}

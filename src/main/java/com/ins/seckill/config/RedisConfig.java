package com.ins.seckill.config;

import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;

/**
 * @author insight
 * @date 2018/08/29
 */
@Configuration
public class RedisConfig extends CachingConfigurerSupport {

    // @Override
    // @Nullable
    // @Bean
    // public KeyGenerator keyGenerator() {
    //     return new RedisCustomKeyGenerator();
    // }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        RedisCustomSerializer customSerializer = new RedisCustomSerializer();
        template.setValueSerializer(customSerializer);
        template.afterPropertiesSet();
        return template;
    }

    /**
     *  设置 redis 数据默认过期时间
     *  设置@cacheable 序列化方式
     * @return
     */
    @Bean
    public RedisCacheConfiguration redisCacheConfiguration(){
        RedisCustomSerializer customSerializer = new RedisCustomSerializer();
        RedisCacheConfiguration configuration = RedisCacheConfiguration.defaultCacheConfig();
        configuration = configuration.serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer
                (customSerializer)).entryTtl(Duration.ofHours(1));
        return configuration;
    }
}

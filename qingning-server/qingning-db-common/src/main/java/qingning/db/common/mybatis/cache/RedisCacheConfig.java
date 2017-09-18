/*
package qingning.db.common.mybatis.cache;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

import java.lang.reflect.Method;

*/
/**
 * Project Name：bdp
 * Type Name：RedisCacheConfig
 * Type Description：
 * Author：Defonds
 * Create Date：2015-09-21
 *//*

@Configuration
@EnableCaching
public class RedisCacheConfig extends CachingConfigurerSupport {
    private static String redis_ip = CacheUtil.getConfigKey("redis.ip");
    private static String redis_port = CacheUtil.getConfigKey("redis.port");
    private static String redis_pass = CacheUtil.getConfigKey("redis.pass");
    private static long defaultCacheTime = 60 * 60 * 2; //默认缓存2小时，保证缓存热点数据

    @Bean
    public JedisConnectionFactory redisConnectionFactory() {
        JedisConnectionFactory redisConnectionFactory = new JedisConnectionFactory();

        // Defaults
        redisConnectionFactory.setHostName(redis_ip);
        redisConnectionFactory.setPort(Integer.valueOf(redis_port));
        redisConnectionFactory.setPassword(redis_pass);
        redisConnectionFactory.setDatabase(2);
        return redisConnectionFactory;
    }

    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory cf) {
        RedisTemplate<String, String> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(cf);
        return redisTemplate;
    }

    @Bean
    public CacheManager cacheManager(RedisTemplate redisTemplate) {
        RedisCacheManager cacheManager = new RedisCacheManager(redisTemplate);
        // Number of seconds before expiration. Defaults to unlimited (0)
        cacheManager.setDefaultExpiration(defaultCacheTime); // Sets the default expire time (in seconds)
        return cacheManager;
    }
    public KeyGenerator customKeyGenerator() {
        return new KeyGenerator() {
            @Override
            public Object generate(Object o, Method method, Object... objects) {
                StringBuilder sb = new StringBuilder();
                sb.append(o.getClass().getName());
                sb.append(method.getName());
                for (Object obj : objects) {
                    sb.append(obj.toString());
                }
                return sb.toString();
            }
        };
    }

}*/

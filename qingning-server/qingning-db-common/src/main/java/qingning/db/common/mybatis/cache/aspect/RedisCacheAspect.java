package qingning.db.common.mybatis.cache.aspect;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import qingning.db.common.mybatis.cache.annotion.RedisCache;
import qingning.db.common.mybatis.cache.annotion.RedisEvict;
import qingning.db.common.mybatis.pageinterceptor.domain.PageBounds;
import qingning.db.common.mybatis.pageinterceptor.domain.PageList;
import qingning.db.common.mybatis.pageinterceptor.domain.Paginator;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by Rouse on 2017/9/27.
 */
@Aspect
@Component
public class RedisCacheAspect {
    private static Logger infoLog = LoggerFactory.getLogger(RedisCacheAspect.class);
    private static final String DELIMITER = ":";
    private static final String PAGE = "page.";
    private static final String PAGE_START = "{limit:";
    private static final String PAGE_CENTER = ",page:";
    private static final String PAGE_END = "}";
    private static final String POINT = ".";
    private static final long EXPIRE_TIME = 3600*3L;

    @Qualifier("redisTemplate")
    @Autowired
    StringRedisTemplate redisTemplate;


    /**
     * 方法调用前，先查询缓存。如果存在缓存，则返回缓存数据，阻止方法调用;
     * 如果没有缓存，则调用业务方法，然后将结果放到缓存中
     * @param jp
     * @return
     * @throws Throwable
     */
    @Around("execution(* qingning.db.common.mybatis.persistence.ShopBannerMapper.select*(..))")
    public Object cache(ProceedingJoinPoint jp) throws Throwable {
        // 得到类名、方法名和参数
        String methodName = jp.getSignature().getName();
        Object[] args = jp.getArgs();

        // 根据类名，方法名和参数生成key
        String hashKey = genHashKey(methodName, args);
        infoLog.debug("生成hashKey:{}", hashKey);
        // 得到被代理的方法
        Method me = ((MethodSignature) jp.getSignature()).getMethod();
        if(me.getAnnotation(RedisCache.class)==null){
            return jp.proceed(args);
        }
        // 得到被代理的方法上的注解
        Class modelType = me.getAnnotation(RedisCache.class).type();
        String fileId = me.getAnnotation(RedisCache.class).hashKey();
        int indexId = me.getAnnotation(RedisCache.class).indexId();
        String redisKey = genKey(modelType.getName());
        if(indexId>-1){
            String id = (String)args[indexId];
            redisKey = redisKey +DELIMITER+ id;
        }else if(StringUtils.isNotEmpty(fileId)){
            String id = ((HashMap<String,Object>)args[0]).get(fileId).toString();
            redisKey = redisKey +DELIMITER+ id;
        }

        // 检查redis中是否有缓存
        String value = (String) redisTemplate.opsForHash().get(redisKey, hashKey);

        // result是方法的最终返回结果
        Object result;
        if (null == value) {
            // 缓存未命中
            infoLog.debug("缓存未命中");
            // 调用数据库查询方法
            result = jp.proceed(args);
            // 序列化查询结果
            String json;
            if(me.getAnnotation(RedisCache.class).pageList()){
                PageList<Map<String, Object>> res = (PageList<Map<String, Object>>)result;
                json = serialize(res);
                JSONObject page = new JSONObject();
                page.put("total_count",res.getTotal());
                page.put("total_page",res.getPaginator().getTotalPages());
                page.put("limit",res.getPaginator().getLimit());
                redisTemplate.opsForHash().put(redisKey, PAGE+hashKey, page.toJSONString());
            }else{
                json = serialize(result);
            }
            // 序列化结果放入缓存
            redisTemplate.opsForHash().put(redisKey, hashKey, json);
            redisTemplate.expire(redisKey,EXPIRE_TIME, TimeUnit.SECONDS);
        } else {
            // 缓存命中
            infoLog.debug("缓存命中, value = {}", value);
            // 得到被代理方法的返回值类型
            Class returnType = ((MethodSignature) jp.getSignature()).getReturnType();

            // 反序列化从缓存中拿到的json
            result = deserialize(value, returnType, modelType);
            if(me.getAnnotation(RedisCache.class).pageList()){
                JSONObject pageJson = JSON.parseObject((String) redisTemplate.opsForHash().get(redisKey, PAGE+hashKey));
                Paginator page = new Paginator(pageJson.getInteger("total_page"),pageJson.getInteger("limit"),pageJson.getInteger("total_count"));
                PageList<Map<String,Object>> list = new PageList<>((List)result,page);
                list.setTotal(pageJson.getInteger("total_count"));
                return list;
            }
            infoLog.debug("反序列化结果 = {}", result);
        }

        return result;
    }
    /**
     * 在方法调用前清除缓存，然后调用业务方法
     * @param jp
     * @return
     * @throws Throwable
     */
    @Around("execution(* qingning.db.common.mybatis.persistence.ShopBannerMapper.insert*(..))" +
            "|| execution(* qingning.db.common.mybatis.persistence.ShopBannerMapper.update*(..))" +
            "|| execution(* qingning.db.common.mybatis.persistence.ShopBannerMapper.delete*(..))")
    public Object evictCache(ProceedingJoinPoint jp) throws Throwable {

        // 得到被代理的方法
        Method me = ((MethodSignature) jp.getSignature()).getMethod();
        // 得到被代理的方法上的注解
        Class modelType = me.getAnnotation(RedisEvict.class).type();
        String key = genKey(modelType.getName());
        String fileId = me.getAnnotation(RedisEvict.class).filedId();
        int indexId = me.getAnnotation(RedisEvict.class).indexId();
        Object[] args = jp.getArgs();
        if(indexId>-1){
            key = key +DELIMITER+ args[indexId];
        }else if(StringUtils.isNotEmpty(fileId)){
            String id = ((HashMap<String,Object>)args[0]).get(fileId).toString();
            key = key +DELIMITER+ id;
        }
        if (infoLog.isDebugEnabled()) {
            infoLog.debug("清空缓存:{}", key);
        }
        // 清除对应缓存
        redisTemplate.delete(key);

        return jp.proceed(jp.getArgs());
    }



    /**
     * 根据类名、方法名和参数生成key
     * @param methodName
     * @param args 方法参数
     * @return
     */
    protected String genHashKey(String methodName, Object[] args) {
        StringBuilder sb = new StringBuilder(methodName);
        sb.append(POINT);
        for (Object obj : args) {
            if(obj instanceof PageBounds){
                sb.append(PAGE_START).append(((PageBounds) obj).getLimit()).append(PAGE_CENTER).append(((PageBounds) obj).getPage()).append(PAGE_END);
            }else{
                sb.append(obj.toString());
                sb.append(POINT);
            }
        }
        return sb.toString();
    }
    /**
     * 根据类名生成key
     * @return
     */
    protected String genKey(String className) {
        String index[] = className.split("\\.");
        return index[index.length-1];
    }

    protected String serialize(Object target) {
        return JSON.toJSONString(target);
    }

    protected Object deserialize(String jsonString, Class clazz, Class modelType) {
        // 序列化结果应该是List对象
        if (clazz.isAssignableFrom(List.class)) {
            return JSON.parseArray(jsonString, modelType);
        }

        // 序列化结果是普通对象
        return JSON.parseObject(jsonString, clazz);
    }
}
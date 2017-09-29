package qingning.db.common.mybatis.cache.annotion;

import java.lang.annotation.*;

/**
 * Created by Rouse on 2017/9/27.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface RedisCache {
    Class type();
    boolean pageList() default false;
    String hashKey() default "";
    int indexId() default -1;
}

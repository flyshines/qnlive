package qingning.db.common.mybatis.cache.annotion;
import java.lang.annotation.*;

/**
 * Created by Rouse on 2017/9/27.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RedisEvict {
    Class type();
    String filedId() default "";
    int indexId() default -1;
}

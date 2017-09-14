package qingning.server.advice.exception;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * 异常相关bean注册类
 */
@Configuration
@EnableAspectJAutoProxy
@ComponentScan(value={
        "qingning.user.server.imp",
        "qingning.saas.server",
        "qingning.lecture.server",
        "qingning.common.server",
        "qingning.common.db.server.imp",
        "qingning.lecturer.db.server.imp",
        "qingning.saas.db.server.impl",
        "qingning.user.db.server.imp",
        })
public class ExceptionConfig {

}
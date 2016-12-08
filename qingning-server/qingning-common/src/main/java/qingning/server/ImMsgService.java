package qingning.server;

import org.springframework.context.ApplicationContext;

import qingning.common.entity.ImMessage;
import qingning.common.util.JedisUtils;

public interface ImMsgService {
	void process(ImMessage message, JedisUtils jedisUtils, ApplicationContext context);
}

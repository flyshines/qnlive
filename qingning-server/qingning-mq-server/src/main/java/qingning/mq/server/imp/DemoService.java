package qingning.mq.server.imp;

import org.springframework.context.ApplicationContext;

import qingning.common.entity.RequestEntity;
import qingning.common.util.JedisUtils;
import qingning.server.AbstractMsgService;

public class DemoService extends AbstractMsgService{

	@Override
	public void process(RequestEntity requestEntity, JedisUtils jedisUtils, ApplicationContext context)
			throws Exception {
		System.out.println("test");
	}
}

package qingning.mq.server.imp;

import org.springframework.context.ApplicationContext;

import qingning.common.entity.ImMessage;
import qingning.common.util.JedisUtils;
import qingning.server.ImMsgService;

public class ImMsgServiceImp implements ImMsgService {

	@Override
	public void process(ImMessage imMessage, JedisUtils jedisUtils, ApplicationContext context) {
		System.out.println("Test");
	}

}

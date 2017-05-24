package qingning.server.rabbitmq;

import java.util.HashMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import qingning.common.entity.RequestEntity;
import qingning.common.util.Constants;
import qingning.common.util.JedisUtils;
import qingning.server.AbstractMsgService;
import qingning.server.ServiceManger;

public class RabbitMQMessageListener implements MessageListener {
	@Autowired
	private MessageConverter messageConverter;
	@Autowired
	private MqThreadPool mqThreadPool;
	@Autowired
	private ApplicationContext context;
	@Autowired
	private JedisUtils jedisUtils;
	
	private HashMap<String,AbstractMsgService> serviceMap = new HashMap<String,AbstractMsgService>();	
	private static Log log = LogFactory.getLog(ServiceManger.class);

	@Override
	public void onMessage(Message message) {
		if(message == null) {
			return;
		}
		RequestEntity requestEntity = (RequestEntity)messageConverter.fromMessage(message);
		boolean asynchronized = Constants.MQ_METHOD_ASYNCHRONIZED.equals(requestEntity.getMethod());
		AbstractMsgService service = serviceMap.get(requestEntity.getServerName());
		if(!serviceMap.containsKey(requestEntity.getServerName())){
			try{
				service = (AbstractMsgService) context.getBean(requestEntity.getServerName());
				serviceMap.put(requestEntity.getServerName(), service);
			} catch(Exception e){
				serviceMap.put(requestEntity.getServerName(), null);
			}
		}
		if(service==null){
			log.info("Service ("+requestEntity.getServerName()+") dosen't exist!");
			return;
		}
		final AbstractMsgService serviceImp = service;
		if(asynchronized){
			Runnable runnable = new Runnable(){
				@Override
				public void run() {
					try{
						serviceImp.invoke(requestEntity, jedisUtils, context);
					} catch (Exception e) {
						log.error(e.getMessage());
					}
				}
			};
			mqThreadPool.execute(runnable);
		} else {
			mqThreadPool.addRequest(requestEntity, service);
		}
	}
}

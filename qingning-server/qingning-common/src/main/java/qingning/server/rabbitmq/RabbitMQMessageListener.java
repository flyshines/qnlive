package qingning.server.rabbitmq;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import qingning.common.entity.RequestEntity;
import qingning.common.util.JedisUtils;
import qingning.common.util.MiscUtils;
import qingning.server.ServiceManger;

public class RabbitMQMessageListener implements MessageListener {
	@Autowired
	private ApplicationContext context;
	@Autowired
	private MessageConverter messageConverter;
	private String propertiesFilePath;
	private SimpleMessageListenerContainer container;
	private JedisUtils jedisUtils;
	private Map<String, MessageServer> messageServerMap = new HashMap<String, MessageServer>();
	private Map<String, List<RequestEntity>> serverQueue = new HashMap<String, List<RequestEntity>>();
	private Map<String, Runnable> serverRunnableMap = new HashMap<String, Runnable>();
	private ThreadPoolExecutor threadPoolExecutor;
	private static Log log = LogFactory.getLog(ServiceManger.class);

	private static int MAX_QUEUQ_SIZE = 2000;

	public RabbitMQMessageListener(){
		int processors = Math.max(Runtime.getRuntime().availableProcessors(),4);		
		threadPoolExecutor = new ThreadPoolExecutor(processors, processors*2, 0, TimeUnit.MILLISECONDS,
				new ArrayBlockingQueue<Runnable>(MAX_QUEUQ_SIZE), new ThreadPoolExecutor.CallerRunsPolicy());
	}

	public void setContext(ApplicationContext context) {
		this.context = context;
	}

	public void setPropertiesFilePath(String propertiesFilePath) throws Exception {
		this.propertiesFilePath = propertiesFilePath;
		if(jedisUtils==null && !MiscUtils.isEmpty(propertiesFilePath)){
			jedisUtils = new JedisUtils(MiscUtils.convertPropertiesFileToMap(this.propertiesFilePath));
		}
	}

	@Override
	public void onMessage(Message message) {
		if(message == null) {
			return;
		}
		if(container==null){
			container=context.getBean(SimpleMessageListenerContainer.class);
		}
		final RequestEntity requestEntity = (RequestEntity)messageConverter.fromMessage(message);
		final String serverName = requestEntity.getServerName();
		if(MiscUtils.isEmpty(serverName)){
			return;
		}

		MessageServer server = messageServerMap.get(serverName);
		if(server==null){
			if(!messageServerMap.containsKey(serverName)){
				server = (MessageServer)context.getBean(serverName);
				messageServerMap.put(serverName, server);
				server.setJedisUtils(jedisUtils);
			} else {
				return;
			}
		}
		List<RequestEntity> queue = serverQueue.get(serverName);
		if(queue==null){
			synchronized(serverQueue){
				queue = serverQueue.get(serverName);
				if(queue==null){
					queue=new LinkedList<RequestEntity>();
					serverQueue.put(serverName, queue);
				}
			}
		}
		synchronized(queue){
			queue.add(requestEntity);
		}
		
		Runnable runnable = new Runnable(){
			@Override
			public void run() {
				try{
					MessageServer server = messageServerMap.get(serverName);
					server.process(requestEntity);
					List<RequestEntity> queue = serverQueue.get(serverName);
					synchronized(queue){
						queue.remove(requestEntity);
					}
				} catch (Exception e) {
					log.error(e.getMessage());
				}
			}
		};
		threadPoolExecutor.execute(runnable);
	}
}

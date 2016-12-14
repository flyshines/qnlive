package qingning.server.rabbitmq;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import qingning.common.entity.RequestEntity;
import qingning.common.util.JedisUtils;
import qingning.server.AbstractMsgService;

public class MqThreadPool {	
	private static int MAX_QUEUQ_SIZE = 2000;
	private static Log log = LogFactory.getLog(MqThreadPool.class);
	
	@Autowired
	private ApplicationContext context;
	@Autowired
	private JedisUtils jedisUtils;
	private ThreadPoolExecutor threadPoolExecutor;
	private HashMap<String,AbstractMsgService> serviceMap = new HashMap<String,AbstractMsgService>();
	private Semaphore semaphore = new Semaphore(1);
	private LinkedList<RequestEntity> requestDeque = new LinkedList<RequestEntity>();
	private Thread thread = new Thread(){
		@Override
		public void run() {
			while(true){

				RequestEntity requestEntity= null;
				synchronized(requestDeque){
					if(!requestDeque.isEmpty()){
						requestEntity = requestDeque.remove();
					}
				}
				if(requestEntity == null){
					try {
						semaphore.acquire();
					} catch (InterruptedException e1) {
						log.error(e1.getMessage());
					}
					continue;
				}
				AbstractMsgService service = serviceMap.get(requestEntity.getServerName());
				try {
					service.invoke(requestEntity, jedisUtils, context);
				} catch (Exception e) {
					log.error(e.getMessage());
				}
			}
		}
	};	
	public MqThreadPool(){
		int processors = Math.max(Runtime.getRuntime().availableProcessors(),4);		
		threadPoolExecutor = new ThreadPoolExecutor(processors, processors*2, 0, TimeUnit.MILLISECONDS,
				new ArrayBlockingQueue<Runnable>(MAX_QUEUQ_SIZE), new ThreadPoolExecutor.CallerRunsPolicy());
		thread.start();
	}
	
	public void execute(Runnable command){
		threadPoolExecutor.execute(command);
	}
	
	public void addRequest(RequestEntity requestEntity, AbstractMsgService service){
		if(requestEntity == null || service == null) return;
		
		synchronized(requestDeque){
			requestDeque.add(requestEntity);
			if(!serviceMap.containsKey(requestEntity.getServerName())){
				serviceMap.put(requestEntity.getServerName(), service);
			}
			semaphore.release();
		}
	}
}

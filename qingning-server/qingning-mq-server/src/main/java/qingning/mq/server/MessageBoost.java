package qingning.mq.server;

import org.springframework.context.support.ClassPathXmlApplicationContext;

public class MessageBoost {
	//private SimpleMessageListenerContainer container;
	
	public static void main(String[] args) throws Exception {
		try(ClassPathXmlApplicationContext content = new ClassPathXmlApplicationContext("mqserver.xml")){		
			try{
				content.start();
				Thread.currentThread().join();
			} finally {
				content.stop();
			}			
		}		
	}
}

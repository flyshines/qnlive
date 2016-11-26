package qingning.common.util;

import org.springframework.amqp.rabbit.core.RabbitTemplate;

import qingning.common.entity.RequestEntity;

public class MqUtils {
	private RabbitTemplate rabbitTemplate;
	public MqUtils(RabbitTemplate rabbitTemplate){
		this.rabbitTemplate=rabbitTemplate;
	}
	public void sendMessage(RequestEntity entity){
		this.rabbitTemplate.convertAndSend(entity);
	}
}

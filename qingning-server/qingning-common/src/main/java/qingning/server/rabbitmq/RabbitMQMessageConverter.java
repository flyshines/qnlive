package qingning.server.rabbitmq;

import java.io.UnsupportedEncodingException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.AbstractMessageConverter;
import org.springframework.amqp.support.converter.MessageConversionException;

import com.alibaba.fastjson.JSON;

import qingning.common.entity.RequestEntity;

public class RabbitMQMessageConverter extends AbstractMessageConverter {
	private static final String DEFAULT_CHARSET = "UTF-8";
	private volatile String defaultCharset = DEFAULT_CHARSET;

	public void setDefaultCharset(String defaultCharset) {
		this.defaultCharset = (defaultCharset != null) ? defaultCharset : DEFAULT_CHARSET;
	}
	@Override
	public RequestEntity fromMessage(Message message) throws MessageConversionException {
		RequestEntity entity = null;
		if(message==null){
			throw new MessageConversionException("the message content is null");
		}
		try {
			entity = JSON.parseObject(new String(message.getBody(),defaultCharset), RequestEntity.class);
		}catch (UnsupportedEncodingException e) {
			throw new MessageConversionException("Failed to read Message content");
		}
		return entity;
	}
	
	@Override
	protected Message createMessage(Object object, MessageProperties messageProperties) {
		byte[] bytes = null;
		if(object==null || !(object instanceof RequestEntity)){
			throw new MessageConversionException("Failed to convert Message content because the object is not RequestEntity");
		}
		try {
			String jsonString = JSON.toJSONString(object);
			bytes = jsonString.getBytes(this.defaultCharset);
		} catch (UnsupportedEncodingException e) {
			throw new MessageConversionException("Failed to convert Message content", e);
		}
		messageProperties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
		messageProperties.setContentEncoding(this.defaultCharset);
		if(bytes != null){
			messageProperties.setContentLength(bytes.length);
		}
		return new Message(bytes, messageProperties);
	}
}
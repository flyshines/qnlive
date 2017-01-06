package qingning.server.rabbitmq;

import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.AbstractMessageConverter;
import org.springframework.amqp.support.converter.MessageConversionException;

import com.alibaba.fastjson.JSON;

import qingning.common.entity.RequestEntity;
import qingning.common.util.Constants;

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
			Object value = entity.getParam();
			if(value != null && value instanceof Map){
				@SuppressWarnings("unchecked")
				Map<String,Object> map = (Map<String,Object>)value;
				if(map.containsKey(Constants.SYS_CLASS_NAME) && map.containsKey(Constants.SYS_CLASS_VALUE)){
					String className = (String)map.get(Constants.SYS_CLASS_NAME);
					entity.setParam(JSON.parseObject(JSON.toJSONString(map.get(Constants.SYS_CLASS_VALUE)),Class.forName(className)));
				}
			}
		}catch (Exception e) {
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
			RequestEntity entity = (RequestEntity)object;
			if(entity.getParam() !=null){
				Object parameter = entity.getParam();
				if(!(parameter instanceof Map) && !(parameter instanceof Collection)){
					Map<String,Object> map = new HashMap<String,Object>();
					map.put(Constants.SYS_CLASS_NAME, parameter.getClass().getName());
					map.put(Constants.SYS_CLASS_VALUE, parameter);
					entity.setParam(map);
				}
			}
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
package qingning.server.rabbitmq;
import java.util.HashMap;
import java.util.Map;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.dom4j.DocumentHelper;
import org.dom4j.Document;
import org.dom4j.Element;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;

import qingning.common.entity.ImMessage;
import qingning.common.util.Constants;
import qingning.common.util.JedisUtils;
import qingning.common.util.MiscUtils;
import qingning.server.ImMsgService;

public class ImRabbitMQMessageListener implements MessageListener {
	private static Logger log = LoggerFactory.getLogger(ImRabbitMQMessageListener.class);
	private static ObjectMapper objectMapper = new ObjectMapper();
	
	@Autowired
	private ApplicationContext context;
	@Autowired
	private JedisUtils jedisUtils;
	@Autowired
	private ImMsgService imMsgService;
	@Autowired
	private MqThreadPool mqThreadPool;	
	
	@Override
	public void onMessage(Message message) {
		String messageStr = null;
		try {			
			messageStr = new String(message.getBody(),"UTF-8");
		} catch (Exception e) {
			log.error(message.getBody() +":" + e.getLocalizedMessage());
		}
		if(MiscUtils.isEmpty(messageStr)){
			return;
		}
		if(!messageStr.startsWith("<?xml ")){
			messageStr="<?xml version=\"1.0\" encoding=\"utf-8\"?>"+messageStr;
		}
		final String msg = messageStr;
		Runnable runnable = new Runnable(){
			@SuppressWarnings("unchecked")
			@Override
			public void run() {
				try {
					String message = MiscUtils.specialCharReplace(msg);
					Document document = DocumentHelper.parseText(message);
					Element element = document.getRootElement();
					Element bodyElement = element.element(Constants.MSG_BODY_ELEMENT);
					if(bodyElement == null){
						return;
					}
					ImMessage imMessage = new ImMessage();
					
					imMessage.setType(MiscUtils.convertString(element.attributeValue(Constants.MSG_TYPE_ATTR)));
					if(!Constants.IM_TYPE_CHAT.equals(imMessage.getType())){
						return;
					}
					imMessage.setMid(MiscUtils.convertString(element.attributeValue(Constants.MSG_MID_ATTR)));
					imMessage.setNewsType(MiscUtils.convertString(element.attributeValue(Constants.MSG_NEWSTYPE_ATTR)));
					imMessage.setIp(MiscUtils.convertString(element.attributeValue(Constants.MSG_IP_ATTR)));
					imMessage.setId(MiscUtils.convertString(element.attributeValue(Constants.MSG_ID_ATTR)));
					
					Element child = element.element(Constants.MSG_FROMJID_ELEMENT);
					if(child!=null){
						imMessage.setFromId(MiscUtils.convertString(child.getText()));
					}
					child = element.element(Constants.MSG_GROUPID_ELEMENT);
					if(child!=null){
						imMessage.setGroupId(MiscUtils.convertString(child.getText()));
					}


						String body = MiscUtils.convertString(bodyElement.getText());
						if(!MiscUtils.isEmpty(body)){
							try{
								imMessage.setBody((Map<String,Object>)objectMapper.readValue(body, Map.class));
							} catch (Exception e){
								Map<String,Object> map = new HashMap<String,Object>();
								map.put(body, body);
								imMessage.setBody(map);
							}
						imMsgService.process(imMessage, jedisUtils, context);
					}
				} catch(Exception e){
					log.error(msg + ":" + e.getMessage());
					return;
				}
			}
		};
		mqThreadPool.execute(runnable);
	}
}

package qingning.server;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import qingning.common.entity.QNLiveException;
import qingning.common.entity.MessageEntity;
import qingning.common.entity.RequestEntity;
import qingning.common.entity.ResponseEntity;
import qingning.common.util.MqUtils;

public abstract class AbstractController {
	private MqUtils mqUtils; 
	@Autowired
	private RabbitTemplate rabbitTemplate;	
	@Autowired
	protected ClassPathXmlApplicationContext applicationContext;
	@Autowired
	protected MessageEntity message;
	@Autowired
	protected ServiceManger serviceManger;

	public RequestEntity createResponseEntity(String serviceName, String function, String accessToken, String version){
		RequestEntity requestEntity = new RequestEntity();
		requestEntity.setAccessToken(accessToken);
		requestEntity.setVersion(version);
		requestEntity.setServerName(serviceName);
		requestEntity.setFunctionName(function);
		requestEntity.setTimeStamp(System.currentTimeMillis());
		return requestEntity;
	}
	
	public ResponseEntity process(RequestEntity reqEntity,ServiceManger serviceManger, MessageEntity message) throws Exception{
		GCWServer servicer = serviceManger.getServer(reqEntity.getServerName(), reqEntity.getVersion());
		if(servicer==null){
			throw new QNLiveException("000001");
		}
		if(mqUtils==null){
			mqUtils = new MqUtils(rabbitTemplate);
		}
		servicer.setApplicationContext(applicationContext);
		servicer.setMqUtils(mqUtils);
		servicer.initRpcServer();
		servicer.validateRequestParamters(reqEntity);
		Object returnValue=servicer.invoke(reqEntity);
		returnValue = servicer.processReturnValue(reqEntity, returnValue);
		ResponseEntity respEntity = new ResponseEntity();
		respEntity.setCode("0");
		respEntity.setMsg(message.getMessages("0"));
		respEntity.setReturnData(returnValue);
		return respEntity;
	}
}

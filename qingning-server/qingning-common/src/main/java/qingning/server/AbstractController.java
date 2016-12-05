package qingning.server;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import org.springframework.util.CollectionUtils;
import qingning.common.entity.QNLiveException;
import qingning.common.entity.MessageEntity;
import qingning.common.entity.RequestEntity;
import qingning.common.entity.ResponseEntity;
import qingning.common.util.MqUtils;
import qingning.server.rpc.manager.ICommonModuleServer;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

	protected Map<String,Object> serverUrlInfoMap;
	private List<Map<String,Object>> serverUrlInfoList;
	protected Long serverUrlInfoUpdateTime;

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
		QNLiveServer servicer = serviceManger.getServer(reqEntity.getServerName(), reqEntity.getVersion());
		if(servicer==null){
			throw new QNLiveException("000001");
		}
		if(mqUtils==null){
			mqUtils = new MqUtils(rabbitTemplate);
		}
		if(reqEntity.getServerName().toString().equals("CommonServer") && CollectionUtils.isEmpty(serverUrlInfoMap)){
			generateServerUrlInfoMap();
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

	private void generateServerUrlInfoMap() {
		ICommonModuleServer iCommonModuleServer = (ICommonModuleServer)applicationContext.getBean("commonModuleServer");
		serverUrlInfoList = iCommonModuleServer.getServerUrls();
		serverUrlInfoMap = new HashMap<String,Object>();

		for(int i = 0; i < serverUrlInfoList.size(); i++){
			Map<String,Object> infoMap = serverUrlInfoList.get(i);

			if(i == 0){
				Date date = (Date)infoMap.get("update_time");
				serverUrlInfoUpdateTime = date.getTime();
			}

			Map<String,Object> innerMap = new HashMap<String,Object>();
			innerMap.put("server_url", infoMap.get("server_url"));
			innerMap.put("method", infoMap.get("method"));
			serverUrlInfoMap.put((String)infoMap.get("server_name"), innerMap);
		}
	}
}

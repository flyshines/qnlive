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
import qingning.server.rpc.manager.IUserModuleServer;

import java.util.*;

public abstract class AbstractController {
	protected MqUtils mqUtils; 
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
	private List<Map<String,Object>> rewardConfigurationList;
	protected Map<String,Object> rewardConfigurationMap;
	protected Long rewardConfigurationTime;
	private List<Map<String,Object>> processRewardConfigurationList;
	//protected List<Map<String,Object>>  classifyInfoList;

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
		if(servicer.isReturnObject(reqEntity)){
			throw new QNLiveException("000103");
		}		
		Object returnValue=process(reqEntity, servicer, serviceManger, message);		
		ResponseEntity respEntity = new ResponseEntity();
		respEntity.setCode("0");
		respEntity.setMsg(message.getMessages("0"));
		respEntity.setReturnData(returnValue);
		return respEntity;
	}

	public Object processWithObjectReturn(RequestEntity reqEntity,ServiceManger serviceManger, MessageEntity message) throws Exception{
		QNLiveServer servicer = serviceManger.getServer(reqEntity.getServerName(), reqEntity.getVersion());
		if(servicer==null){
			throw new QNLiveException("000001");
		}
		if(!servicer.isReturnObject(reqEntity)){
			throw new QNLiveException("000103");
		}
		return process(reqEntity, servicer, serviceManger, message);
	}
	
	protected void initControlResource(){
		if(mqUtils==null){
			mqUtils = new MqUtils(rabbitTemplate);
		}
	}
	
	private Object process(RequestEntity reqEntity, QNLiveServer servicer,ServiceManger serviceManger, MessageEntity message) throws Exception{
		initControlResource();
		if(reqEntity.getServerName().toString().equals("CommonServer") && CollectionUtils.isEmpty(serverUrlInfoMap)){
			generateServerUrlInfoMap();
		}
		if(reqEntity.getServerName().toString().equals("UserServer") && CollectionUtils.isEmpty(rewardConfigurationMap)){
			generateRewardConfigurationMap();
		}
		servicer.setApplicationContext(applicationContext);
		servicer.setMqUtils(mqUtils);
		servicer.initRpcServer();
		servicer.validateRequestParamters(reqEntity);
		Object returnValue=servicer.invoke(reqEntity);
		return servicer.processReturnValue(reqEntity, returnValue);
	}
	
	private void generateRewardConfigurationMap(){
		IUserModuleServer userModuleServer = (IUserModuleServer)applicationContext.getBean("userModuleServer");
		rewardConfigurationList = userModuleServer.findRewardConfigurationList();
		rewardConfigurationMap = new HashMap<>();
		processRewardConfigurationList = new ArrayList<>();

		if(rewardConfigurationList != null){
			for(int i = 0; i < rewardConfigurationList.size(); i++){
				Map<String,Object> infoMap = rewardConfigurationList.get(i);

				if(i == 0){
					Date date = (Date)infoMap.get("update_time");
					rewardConfigurationTime = date.getTime();
				}

				Map<String,Object> innerMap = new HashMap<String,Object>();
				innerMap.put("reward_id", infoMap.get("reward_id"));
				innerMap.put("amount", (Long)infoMap.get("amount")/100.0);
				innerMap.put("reward_pos", infoMap.get("reward_pos"));
				processRewardConfigurationList.add(innerMap);
			}
		}


		if(rewardConfigurationList != null && rewardConfigurationList.size() > 0){
			rewardConfigurationMap.put("reward_update_time", rewardConfigurationTime);
			rewardConfigurationMap.put("reward_list", processRewardConfigurationList);
		}
	}

	private void generateServerUrlInfoMap() {
		ICommonModuleServer iCommonModuleServer = (ICommonModuleServer)applicationContext.getBean("commonModuleServer");
		//classifyInfoList = iCommonModuleServer.findClassifyInfo();
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
			innerMap.put("protocol", infoMap.get("protocol"));
			innerMap.put("domain_name", infoMap.get("domain_name"));
			serverUrlInfoMap.put((String)infoMap.get("server_name"), innerMap);
		}


		rewardConfigurationList = iCommonModuleServer.findRewardConfigurationList();
		rewardConfigurationMap = new HashMap<>();
		processRewardConfigurationList = new ArrayList<>();

		if(rewardConfigurationList != null){
			for(int i = 0; i < rewardConfigurationList.size(); i++){
				Map<String,Object> infoMap = rewardConfigurationList.get(i);

				if(i == 0){
					Date date = (Date)infoMap.get("update_time");
					rewardConfigurationTime = date.getTime();
				}

				Map<String,Object> innerMap = new HashMap<String,Object>();
				innerMap.put("reward_id", infoMap.get("reward_id"));
				innerMap.put("amount", (Long)infoMap.get("amount")/100.0);
				innerMap.put("reward_pos", infoMap.get("reward_pos"));
				processRewardConfigurationList.add(innerMap);
			}
		}


		if(rewardConfigurationList != null && rewardConfigurationList.size() > 0){
			rewardConfigurationMap.put("reward_update_time", rewardConfigurationTime);
			rewardConfigurationMap.put("reward_list", processRewardConfigurationList);
		}



	}
}

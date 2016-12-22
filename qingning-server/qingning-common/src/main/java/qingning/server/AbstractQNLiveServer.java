package qingning.server;

import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.context.ApplicationContext;

import qingning.common.entity.FunctionInfo;
import qingning.common.entity.QNLiveException;
import qingning.common.entity.InputParameter;
import qingning.common.entity.OutputParameter;
import qingning.common.entity.RequestEntity;
import qingning.common.util.Constants;
import qingning.common.util.JedisUtils;
import qingning.common.util.MiscUtils;
import qingning.common.util.MqUtils;
import qingning.server.annotation.FunctionName;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;

public abstract class AbstractQNLiveServer implements QNLiveServer {
	private Map<String, FunctionInfo> functionInfoMap = new HashMap<String, FunctionInfo>(); 
	private Map<String, Method> functionInfoMethodMap = new HashMap<String, Method>();
	
	protected ApplicationContext context;
	protected JedisUtils jedisUtils;
	protected MqUtils mqUtils;
	
	
	/**
	 * 查询系统级别缓存
	 * @param jedis
	 * @param keyIds        id集合的key值
	 * @param objectKeyId   对象key值的SYS:BILL:前缀  如："SYS:BILL:"+订单号
	 * @param num 当前页数
	 * @param pageCount  每页条数
	 * @return
	 */
	protected List<Map<String, String>> sysPagedQuery(String keyIds, String objectKeyId, Long num, Long pageCount) {
		List<Map<String, String>> list = new ArrayList<Map<String, String>>();
		JedisBatchCallback jedisBatchCallback = (JedisBatchCallback) jedisUtils.getJedis();
		jedisBatchCallback.invoke(new JedisBatchOperation() {
			@Override
			public void batchOperation(Pipeline pipeline, Jedis jedis) {
				// 开始记录数
				Long startCount = (num - 1) * pageCount;
				List<String> lrange = jedis.lrange(keyIds, startCount, startCount + pageCount - 1);
				if (startCount.intValue() != 0 && lrange.size() != pageCount) {
					// 当缓存中数据不够时直接返回空
					return;
				}
				// 使用pipeline 批量获取数据
				// Pipeline p = jedis.pipelined();
				Map<String, Response<Map<String, String>>> responses = new HashMap<String, Response<Map<String, String>>>();
				for (String key : lrange) {
					String objectKey = objectKeyId + key;
					responses.put(key, pipeline.hgetAll(objectKey));
				}
				// 同步数据
				pipeline.sync();
				for (String key : responses.keySet()) {
					Map<String, String> map = new HashMap<String, String>();
					// 获取订单对象
					map = responses.get(key).get();
					list.add(map);
				}
			}
		});

		return list;
	}

	
	
	public AbstractQNLiveServer(){
		if(functionInfoMethodMap.isEmpty()){
			Method[] methods = getClass().getDeclaredMethods();
			for(Method method:methods){
				FunctionName  functionName = method.getAnnotation(FunctionName.class);
				if(functionName==null){
					continue;
				}
				String value = functionName.value();
				if(MiscUtils.isEmpty(value)){
					continue;
				}
				Class<?>[] cls = method.getParameterTypes();
				if(cls==null || cls.length != 1){
					continue;
				}
				if(cls[0]!=RequestEntity.class){
					continue;
				}
				method.setAccessible(true);
				functionInfoMethodMap.put(value.trim(), method);
			}
		}
	}
	
	@SuppressWarnings({ "rawtypes"})
	private void validateFormat(FunctionInfo functionInfo, Object inputParameterObj) throws Exception{
		if(inputParameterObj instanceof Map){
			Map inputParameterMap = (Map)inputParameterObj;
			for(InputParameter inputParamter : functionInfo.getInputParameterList()){
				Object value = inputParameterMap.get(inputParamter.getName());
				if(inputParamter.isRequire() && MiscUtils.isEmpty(value)){
					throw new QNLiveException(inputParamter.getRequireErrorCode());
				}
				
				if(!MiscUtils.isEmpty(inputParamter.getFormat()) && !MiscUtils.isEmpty(value)){
					if(value instanceof String){
						String valueStr = (String)value;
						if(!Pattern.matches(inputParamter.getFormat(), valueStr)){
							throw new QNLiveException(inputParamter.getFormatErrorCode());
						}
					}
				}
				List<String> condition= inputParamter.getCheckCondition();
				if(!condition.isEmpty()){
					boolean check=false;
					Map<String,Object> conditionValue = new HashMap<String,Object>();
					for(String key:condition){
						Object currValue = inputParameterMap.get(key);
						if(!MiscUtils.isEmpty(currValue)){
							check=true;
							conditionValue.put(key, currValue);
						}
					}
					if(check){
						inputParamter.checkCondition(conditionValue);
					}
				}				
			}
		}
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void convertStringToObject(FunctionInfo functionInfo, Object inputParameterObj) throws Exception{
		if(inputParameterObj instanceof Map){
			Map inputParameterMap = (Map)inputParameterObj;
			for(InputParameter inputParamter : functionInfo.getInputParameterList()){
				inputParameterMap.put(inputParamter.getName(), MiscUtils.convertStringToObject(inputParameterMap.get(inputParamter.getName()), inputParamter.getType(), inputParamter.getName()));
			}
		}
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void procRtnValue(FunctionInfo functionInfo, Object outputValue) throws Exception{		
		if(outputValue instanceof Map){
			Map outputValueMap = (Map)outputValue;
			Map retValue = new HashMap();
			for(OutputParameter outputParameter : functionInfo.getOutputParameterList()){
				String fieldName = outputParameter.getFieldName();
				String name = outputParameter.getName();
				Object value = null;
				if(!MiscUtils.isEmpty(fieldName)){
					value=outputValueMap.get(fieldName);
				} else {
					value=outputValueMap.get(name);
				}
				value = outputParameter.convertValue(value);
				retValue.put(name, value);
			}
			outputValueMap.clear();
			for(Object key:retValue.keySet()){
				outputValueMap.put(key, retValue.get(key));
			}
		}	
	}
	
	@SuppressWarnings("unchecked")
	protected <T> T getRpcService(String name){
		T rpcService = null;
		if(context!=null && !MiscUtils.isEmpty(name)){
			rpcService=(T)context.getBean(name);
		}
		return rpcService;
	}
	
	
	
	@Override
	public void addFunctionInfo(FunctionInfo functionInfo) {
		if(functionInfo != null && !MiscUtils.isEmpty(functionInfo.getFunctionName())){
			functionInfoMap.put(functionInfo.getFunctionName(), functionInfo);
		}
	}

	@Override
	public void setMqUtils(MqUtils mqUtils) {
		this.mqUtils=mqUtils;
	}

	@Override
	public void setJedisUtils(JedisUtils jedisUtils){
		this.jedisUtils=jedisUtils;
	}

	@Override
	public void setApplicationContext(ApplicationContext context) {
		this.context = context;
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	public Object processReturnValue(RequestEntity reqEntity, Object outputValue) throws Exception{
		FunctionInfo functionInfo = this.functionInfoMap.get(reqEntity.getFunctionName());
		if(outputValue == null || functionInfo.getOutputParameterList().isEmpty()){
			return outputValue;
		}
		if(outputValue instanceof Map){
			procRtnValue(functionInfo, outputValue);
		} else if(outputValue instanceof Collection){
			Collection outputValueCol = (Collection)outputValue;			
			if(!outputValueCol.isEmpty()){
				for(Object obj : outputValueCol){
					procRtnValue(functionInfo, obj);
				}
			}
		}
		return outputValue;
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	public void validateRequestParamters(RequestEntity reqEntity) throws Exception {
		
		String functionName = reqEntity.getFunctionName();
		FunctionInfo functionInfo = null;
		if(!MiscUtils.isEmpty(functionName)){
			functionInfo = this.functionInfoMap.get(functionName);
		}
		
		if(functionInfo == null){
			throw new QNLiveException("000001");
		}
		
		
		String accessToken = reqEntity.getAccessToken();
		Map<String,Object> map = new HashMap<String,Object>();
		map.put(Constants.CACHED_KEY_ACCESS_TOKEN_FIELD, accessToken);
		String process_access_token = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_ACCESS_TOKEN, map);
		String accessTokenKey=process_access_token;

		if(functionInfo.isAccessTokenRequire()){
			if(MiscUtils.isEmpty(accessToken)){		
				throw new QNLiveException("000003");
			}
			//TODO check accessToken
			//从redis中获取accessToken，取到则代表验证通过并且顺延时间，取不到则代表验证失败
			Jedis jedis = jedisUtils.getJedis();
			if(jedis != null){
				if(jedis.exists(accessTokenKey)){
					//将accessTokenKey有效期顺延3个小时
					jedis.expire(accessTokenKey, 10800);
				}else {
					throw new QNLiveException("000003");
				}
			}else{
				throw new QNLiveException("000005");
			}
		}
		

		if(functionInfo.isTimesLimitsRequire() && !MiscUtils.isEmpty(accessToken)){
			Jedis jedis = jedisUtils.getJedis();
			if(jedis != null){
				long currentTime = System.currentTimeMillis();
				String funName=reqEntity.getServerName()+"-"+reqEntity.getFunctionName();
				if(jedis.exists(accessTokenKey)){
					String lastVisitFun = jedis.hget(accessTokenKey, Constants.LAST_VISIT_FUN);
					if(!MiscUtils.isEmpty(lastVisitFun) && lastVisitFun.equals(funName)){
						long lastTime = -1;
						try{
							lastTime = Long.parseLong(jedis.hget(accessTokenKey, Constants.LAST_VISIT_TIME));
						}catch(Exception e){
							lastTime = -1;
						}
						if(lastTime> 0 && currentTime-lastTime<=functionInfo.getTimesLimits()){
							jedis.hset(accessTokenKey, Constants.LAST_VISIT_TIME,String.valueOf(currentTime));
							throw new QNLiveException("000002");
						}
					}
				}
				((JedisBatchCallback)jedis).invoke(new JedisBatchOperation(){
					@Override
					public void batchOperation(Pipeline pipeline,Jedis jedis) {
						pipeline.hset(accessTokenKey, Constants.LAST_VISIT_FUN,funName);
						pipeline.hset(accessTokenKey, Constants.LAST_VISIT_TIME,String.valueOf(currentTime));
						pipeline.sync();
					}
				});
			}
		}
		Object inputParameterObj = reqEntity.getParam();
		if(inputParameterObj==null){
			inputParameterObj=new HashMap<String,String>();
		}		
		if(inputParameterObj instanceof Collection){
			Collection inputParameterCol = (Collection)inputParameterObj;
			if(!inputParameterCol.isEmpty()){
				for(Object obj : inputParameterCol){
					convertStringToObject(functionInfo, obj);
				}
				
				for(Object obj : inputParameterCol){
					validateFormat(functionInfo, obj);
				}
			}
		} else {
			convertStringToObject(functionInfo, inputParameterObj);
			validateFormat(functionInfo, inputParameterObj);
		}		
	}
	
	public Object process(RequestEntity reqEntity) throws Exception{
		return null;
	}
	
	public final Object invoke(RequestEntity reqEntity) throws Exception{
		Method method = functionInfoMethodMap.get(reqEntity.getFunctionName());
		if(method != null){
			return method.invoke(this, reqEntity);
		} else {
			return this.process(reqEntity);
		}
	}
	
	public String getAutoNumber(String key){
		String result = null;
		Jedis jedis = jedisUtils.getJedis();
		if(jedis.exists(key)){
			result = jedis.get(key);
			jedis.set(key, String.valueOf(Integer.parseInt(result)+1));
		}else{
			result = "100000";
			jedis.set(key, String.valueOf(Integer.parseInt(result)+1));
		}
		return result;
	}
	
	public void updateCache(String key, Map<String, Object> inMap){
		Jedis jedis = jedisUtils.getJedis();
		Map<String, String> map = new HashMap<String, String>();
		for(Map.Entry<String, Object> entry : inMap.entrySet()) {
		   if(!MiscUtils.isEmpty(entry.getValue())){
			   if("start_date".equals(entry.getKey()) || "end_date".equals(entry.getKey())){
				   map.put(entry.getKey(), new SimpleDateFormat("yyyy-MM-dd").format(entry.getValue()));
			   }else if("start_time".equals(entry.getKey()) || "end_time".equals(entry.getKey())){
				   map.put(entry.getKey(), new SimpleDateFormat("HH:mm:ss").format(entry.getValue()));
			   }else{
				   if(entry.getValue() instanceof Date){
					   map.put(entry.getKey(), new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(entry.getValue()));
				   }else{
					   map.put(entry.getKey(), entry.getValue().toString());
				   }
			   }
		   }
		}
		jedis.hmset(key, map);
	}

	protected RequestEntity generateRequestEntity(String serverName, String method, String functionName, Object param) {
		RequestEntity requestEntity = new RequestEntity();
		requestEntity.setServerName(serverName);
		requestEntity.setMethod(method);
		requestEntity.setFunctionName(functionName);
		requestEntity.setParam(param);
		return requestEntity;
	}
}

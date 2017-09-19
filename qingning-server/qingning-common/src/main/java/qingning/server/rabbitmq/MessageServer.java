package qingning.server.rabbitmq;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import qingning.common.entity.RequestEntity;
import qingning.common.util.JedisUtils;
import qingning.common.util.MiscUtils;
import qingning.server.JedisBatchCallback;
import qingning.server.JedisBatchOperation;

public abstract class MessageServer {

	@Autowired(required=true)
	protected JedisUtils  jedisUtils;
	
	public static final Integer SECONDS=3*60*60*24;
	 
	public abstract void  process(RequestEntity requestEntity) throws Exception;
	
	public void setJedisUtils(JedisUtils jedisUtils) {
		this.jedisUtils = jedisUtils;
	}

	/**
	 * @param list
	 * @param keyIds  	   redis ids集合的 的key名称
	 * @param objectKeyId  redis 对象的 的key名称
	 * @param majorKey	   list 的主键名称
	 */
	protected void cacheObjectInfo(List<Map<String, Object>> list,String keyIds,String objectKeyId,String majorKey) {
		JedisBatchCallback jedisBatchCallback=(JedisBatchCallback)jedisUtils.getJedis();
		jedisBatchCallback.invoke(new JedisBatchOperation(){
			@Override
			public void batchOperation(Pipeline pipeline, Jedis jedis) {
				jedis.del(keyIds);
				for (Map<String, Object> object : list) {
					//存储 对象 信息
					pipeline.rpush(keyIds, object.get(majorKey).toString());
					//获取封装好的对象数据
					Map<String, String> billMap = getEncapsulationObject(object);
					String key=objectKeyId+object.get(majorKey);
					pipeline.hmset(key, billMap);
					pipeline.expire(key, SECONDS);
				}
				pipeline.expire(keyIds, SECONDS);
				pipeline.sync();
			}
		});
	}
	
	protected Map<String, String> getEncapsulationObject(Map<String, Object> object){
		Map<String, String> billMap = new HashMap<String, String>();
		for(String key:object.keySet()){
			billMap.put(key, MiscUtils.convertString(object.get(key)));
		}
		return billMap;
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

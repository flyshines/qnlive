package qingning.common.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qingning.common.entity.RequestEntity;
import qingning.server.JedisBatchCallback;
import qingning.server.JedisBatchOperation;
import qingning.server.rpc.CommonReadOperation;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;

import java.util.*;

public final class CacheUtils {
	private static Logger log = LoggerFactory.getLogger(CacheUtils.class);
	private static ObjectMapper objectMapper = new ObjectMapper();
	
	private static Map<String,String> readData(String searchKey, String keyFormat, String keyField, 
			RequestEntity requestEntity, CommonReadOperation operation, JedisUtils jedisUtils, boolean cachedValue) throws Exception{
		return readData(searchKey, keyFormat, keyField, requestEntity, operation, jedisUtils, cachedValue, -1);
	}

	private static Map<String,String> readData(String searchKey, String keyFormat, String keyField, 
			RequestEntity requestEntity, CommonReadOperation operation, JedisUtils jedisUtils, boolean cachedValue, int lifeTime) throws Exception{
		String[] searchKeys={searchKey};
		String[] keyFields={keyField};
		return readData(searchKeys, keyFormat, keyFields, requestEntity, operation, jedisUtils, cachedValue, lifeTime);
	}
	
	@SuppressWarnings({"rawtypes", "unchecked" })
	private static Map<String,String> readData(String[] searchKeys, String keyFormat, String[] keyFields, 
			RequestEntity requestEntity, CommonReadOperation operation, JedisUtils jedisUtils, boolean cachedValue, int lifeTime) throws Exception{
		boolean useCached = jedisUtils!=null;
		Map<String,String> dataValue = null;
		Map<String, String> keyMap = new HashMap<String, String>();
		int length = searchKeys.length;
		for(int i = 0; i < length; ++i){
			keyMap.put(keyFields[i], searchKeys[i]);
		}		
		String key = MiscUtils.getKeyOfCachedData(keyFormat, keyMap);
		Jedis jedis = null;
		if(useCached){
			jedis = jedisUtils.getJedis();
			dataValue=jedis.hgetAll(key);
		}
		
		if(MiscUtils.isEmpty(dataValue)){
			Map result = (Map) operation.invokeProcess(requestEntity);
			if(!MiscUtils.isEmpty(result)){
				dataValue = new HashMap<String,String>();
				MiscUtils.converObjectMapToStringMap(result, dataValue);
				if(cachedValue){
					jedis.hmset(key, dataValue);					
				}
			}
		}
		if(cachedValue && lifeTime > 0 && !MiscUtils.isEmpty(dataValue)){
			jedis.expire(key, lifeTime);
		}
		return dataValue;
	}
	/*
	@SuppressWarnings({"rawtypes" })
	private static List<Map<String,String>> readListFromDB(String keyFormat, String listKey, String sortKey, String primaryKey, RequestEntity requestEntity, 
			CommonReadOperation operation, JedisUtils jedisUtils, boolean cachedList) throws Exception{
		boolean useCached = jedisUtils!=null && cachedList;		
		List result = (List) operation.invokeProcess(requestEntity);
		
		List<Map<String,String>> listValue = new LinkedList<Map<String,String>>();
		if(result != null){
			for(Object obj:result){
				if(obj instanceof Map){
					Map<String, String> objMap = new HashMap<String, String>();
					Map map = (Map)obj;
					for(Object mapKey:map.keySet()){
						objMap.put((String)mapKey, MiscUtils.convertString(map.get(mapKey)));						
					}
					listValue.add(objMap);
				}
			}
			if(useCached && !MiscUtils.isEmpty(listValue)){
    			JedisBatchCallback callBack = (JedisBatchCallback)jedisUtils.getJedis();
    			final List innerList = listValue;
    			callBack.invoke(new JedisBatchOperation(){
					@Override
					public void batchOperation(Pipeline pipeline, Jedis jedis) {
						int length = innerList.size();
                        boolean sortList = !MiscUtils.isEmpty(sortKey) && !MiscUtils.isEmpty(listKey);			
						for(int i = 0; i < length; ++i){
							Map values = (Map)innerList.get(i);
							Map<String,String> strMap = new HashMap<String,String>();
							for(Object key:values.keySet()){
								strMap.put((String)key,MiscUtils.convertString(values.get(key)));
							}
							pipeline.hmset(MiscUtils.getKeyOfCachedData(keyFormat,strMap), strMap);
							if(sortList){
								double score = Double.parseDouble(MiscUtils.convertString(values.get(sortKey)));
								pipeline.zadd(listKey, score, MiscUtils.convertString(values.get(primaryKey)));
							}
						}
						pipeline.sync();
					}
    			});
			}
		}
		return listValue;
	}
	*/
	public static Map<String,String> readUserNoCache(String userId, RequestEntity requestEntity, 
			CommonReadOperation operation, JedisUtils jedisUtils) throws Exception{
		Map<String,String> result = readData(userId, Constants.CACHED_KEY_USER, Constants.CACHED_KEY_USER_FIELD, requestEntity, operation, jedisUtils, false, -1);
		return result;
	}
	
	public static Map<String,String> readUser(String userId, RequestEntity requestEntity, 
			CommonReadOperation operation, JedisUtils jedisUtils) throws Exception{
		Map<String,String> result = readData(userId, Constants.CACHED_KEY_USER, Constants.CACHED_KEY_USER_FIELD, requestEntity, operation, jedisUtils, true, 60*60*72);
		return result;
	}
	
	public static Map<String,String> readLecturer(String lecturerId, RequestEntity requestEntity, 
			CommonReadOperation operation, JedisUtils jedisUtils) throws Exception{					
		Map<String,String> values = readData(lecturerId, Constants.CACHED_KEY_LECTURER, Constants.CACHED_KEY_LECTURER_FIELD, requestEntity, operation, jedisUtils,true);
		/*
		if(jedisUtils != null){
			Jedis jedis = jedisUtils.getJedis();			
			Map<String, String> keyMap = new HashMap<String, String>();
			keyMap.put(Constants.CACHED_KEY_LECTURER_FIELD, lecturerId);
			String key = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER_ROOMS, keyMap);
			if(!jedis.exists(key)){
				String functionName = requestEntity.getFunctionName();
				requestEntity.setFunctionName(Constants.LECTURER_ROOM_LOAD);				
				readListFromDB(Constants.CACHED_KEY_SPECIAL_LECTURER_ROOM, key, Constants.FIELD_CREATE_TIME, Constants.FIELD_ROOM_ID,requestEntity, operation, jedisUtils, true);
				requestEntity.setFunctionName(functionName);
			}
		}
		*/
		return values;
	}
	
	public static Map<String,String> readCourse(String course_id, RequestEntity requestEntity, 
			CommonReadOperation operation, JedisUtils jedisUtils,boolean cachedValue) throws Exception{
		return readData(course_id, Constants.CACHED_KEY_COURSE, Constants.CACHED_KEY_COURSE_FIELD, requestEntity, operation, jedisUtils, cachedValue);
	}
	
	@SuppressWarnings("unchecked")
	public static Map<String,String> readFullCourseInfo(String course_id, RequestEntity requestEntity, 
			CommonReadOperation operation, JedisUtils jedisUtils) throws Exception{
		Map<String,String> values = readData(course_id, Constants.CACHED_KEY_COURSE, Constants.CACHED_KEY_COURSE_FIELD, requestEntity, operation, jedisUtils, false);
		boolean useCached = jedisUtils!=null;
		if(!values.containsKey(Constants.CACHED_KEY_COURSE_PPTS_FIELD)){
			String pptJsonList = null;
			if(useCached){
				Map<String, String> keyMap = new HashMap<String, String>();
				keyMap.put(Constants.CACHED_KEY_COURSE_FIELD, course_id);
				String key = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_PPTS, keyMap);
				Jedis jedis = jedisUtils.getJedis();
				pptJsonList = jedis.get(key);						
			}
			List<Map<String,Object>> list = null;
			if(MiscUtils.isEmpty(pptJsonList)){
				//TODO requestEntity. function
				list = (List<Map<String,Object>>)operation.invokeProcess(requestEntity);
				if(MiscUtils.isEmpty(list)){
					list = new LinkedList<Map<String,Object>>();
				}
				pptJsonList = objectMapper.writeValueAsString(list);
			}
			values.put(Constants.CACHED_KEY_COURSE_PPTS_FIELD, pptJsonList);
		}
		if(!values.containsKey(Constants.CACHED_KEY_COURSE_AUDIOS_FIELD)){
			String audiosJsonList = null;
			if(useCached){
				Map<String, String> keyMap = new HashMap<String, String>();
				keyMap.put(Constants.CACHED_KEY_COURSE_AUDIOS, course_id);
				String key = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_AUDIOS, keyMap);
				Jedis jedis = jedisUtils.getJedis();
				audiosJsonList = jedis.get(key);						
			}
			List<Map<String,Object>> list = null;
			if(MiscUtils.isEmpty(audiosJsonList)){
				//TODO requestEntity. function
				list = (List<Map<String,Object>>)operation.invokeProcess(requestEntity);
				if(MiscUtils.isEmpty(list)){
					list = new LinkedList<Map<String,Object>>();
				}
				audiosJsonList = objectMapper.writeValueAsString(list);
			}
			values.put(Constants.CACHED_KEY_COURSE_PPTS_FIELD, audiosJsonList);
		}
		return values;
	}

	public static Map<String,String> readLiveRoom(String room_id, RequestEntity requestEntity,
												CommonReadOperation operation, JedisUtils jedisUtils,boolean cachedValue) throws Exception{
		Map<String,String> values =  readData(room_id, Constants.CACHED_KEY_ROOM, Constants.FIELD_ROOM_ID, requestEntity, operation, jedisUtils, cachedValue);
		if(!MiscUtils.isEmpty(values) && cachedValue){
	        Map<String, Object> map = new HashMap<String, Object>();
	        map.put(Constants.CACHED_KEY_LECTURER_FIELD, values.get(Constants.CACHED_KEY_LECTURER_FIELD));
	        String lectureLiveRoomKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER_ROOMS, map);
	        jedisUtils.getJedis().hset(lectureLiveRoomKey, room_id, "1");
		}
		return values;
	}
	
	public static String readLiveRoomInfoFromCached(String room_id, String fieldName,RequestEntity requestEntity,
			CommonReadOperation operation, JedisUtils jedisUtils,boolean cachedValue) throws Exception{
		if(MiscUtils.isEmpty(fieldName)){
			return "";
		}
		
		Map<String, String> keyMap = new HashMap<String, String>();
		keyMap.put(Constants.FIELD_ROOM_ID, room_id);
		String key = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_ROOM, keyMap);
		Jedis jedis = jedisUtils.getJedis();
		String result = jedis.hget(key, fieldName); 
		if(MiscUtils.isEmpty(result) && !jedis.exists(key)){
			Map<String,String> values = readLiveRoom(room_id,requestEntity,operation,jedisUtils,cachedValue);
			if(!MiscUtils.isEmpty(values)){
				result = values.get(fieldName);
			}
		}
		return result;
	}
	
	public static Map<String,String> readDistributer(String distributer_id, RequestEntity requestEntity,
			CommonReadOperation operation, JedisUtils jedisUtils,boolean cachedValue) throws Exception{
		return readData(distributer_id, Constants.CACHED_KEY_DISTRIBUTER, Constants.CACHED_KEY_DISTRIBUTER_FIELD, requestEntity, operation, jedisUtils, cachedValue);
	}

	public static Map<String,String> readDistributerRoom(String distributer_id, String room_id, CommonReadOperation operation, JedisUtils jedisUtils) throws Exception{
		String[] searchKeys={distributer_id,room_id};
		String[] keyFields={Constants.CACHED_KEY_DISTRIBUTER_FIELD,Constants.FIELD_ROOM_ID};
		RequestEntity requestEntity = new RequestEntity();
		int len = searchKeys.length;
		Map<String,String> query = new HashMap<String,String>();
		for(int i=0; i<len; ++i){
			query.put(keyFields[i], searchKeys[i]);
		}
		requestEntity.setParam(query);
		return readData(searchKeys, Constants.CACHED_KEY_ROOM_DISTRIBUTER, keyFields, requestEntity, operation, jedisUtils, true, -1);
	}
	
	public static Map<String,String> readAppVersion(String os, RequestEntity requestEntity,
													 CommonReadOperation operation, JedisUtils jedisUtils,boolean cachedValue) throws Exception{
		return readData(os, Constants.CACHED_KEY_APP_VERSION_INFO, Constants.CACHED_KEY_APP_VERSION_INFO_FIELD, requestEntity, operation, jedisUtils, cachedValue);
	}

	
	public static List<Map<String,String>> readCourseListInfoOnlyFromCached(JedisUtils jedisUtils, List<String> courseIdList,
			CommonReadOperation operation){
		final List<Map<String,String>> result = new ArrayList<Map<String,String>>();
		if(MiscUtils.isEmpty(courseIdList)){
			return result;
		}
		JedisBatchCallback callBack = (JedisBatchCallback)jedisUtils.getJedis();
		
		callBack.invoke(new JedisBatchOperation(){
			@SuppressWarnings("unchecked")
			@Override
			public void batchOperation(Pipeline pipeline, Jedis jedis) {
				Map<String, Response<Map<String,String>>> cachedMap = new HashMap<String, Response<Map<String,String>>>();
				Map<String, String> keyMap = new HashMap<String, String>();				
				for(String courseId:courseIdList){
					keyMap.clear();
					keyMap.put(Constants.CACHED_KEY_COURSE_FIELD, courseId);
					String key = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE, keyMap);					
					cachedMap.put(courseId, pipeline.hgetAll(key));					
				}
				pipeline.sync();
				if(!cachedMap.isEmpty()){
					RequestEntity requestEntity = new RequestEntity();
					for(String courseId:courseIdList){
						Response<Map<String,String>> value = cachedMap.get(courseId);
						Map<String,String> courseValue = null;
						if(MiscUtils.isEmpty(value) || MiscUtils.isEmpty(value.get())) {
							Map<String,Object> param = new HashMap<String,Object>();
							param.put(Constants.CACHED_KEY_COURSE_FIELD, courseId);
							requestEntity.setParam(param);
							courseValue = new HashMap<String,String>();
							try {
								MiscUtils.converObjectMapToStringMap((Map<String,Object>)operation.invokeProcess(requestEntity), courseValue);
								if(!MiscUtils.isEmpty(courseValue)){
									keyMap.clear();
									keyMap.put(Constants.CACHED_KEY_COURSE_FIELD, courseId);
									String key = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE, keyMap);
									pipeline.hmset(key, courseValue);
								}
							} catch (Exception e) {
								courseValue = null;
							}
						} else {
							courseValue = value.get();
						}
						if(MiscUtils.isEmpty(courseValue)){
							continue;
						}
						result.add(courseValue);
					}
				}
			}
		});
		return result;
	}
	
	@SuppressWarnings("unchecked")
	public static Map<String,Object> convertCachedStringToMap(String valueStr){
		if(MiscUtils.isEmptyString(valueStr)) return null;
		Map<String, Object> value = null;
		try {
			value = objectMapper.readValue(valueStr, Map.class);
		} catch (Exception e) {
			log.warn(e.getMessage());
			value = null;
		}
		return value;
	}
	public static String convertMaptoCachedString(Map<String,Object> values){
		if(MiscUtils.isEmpty(values)) return "";		
		try {
			Map<String,String> valueStrMap = new HashMap<String,String>();
			MiscUtils.converObjectMapToStringMap(values, valueStrMap);
			return objectMapper.writeValueAsString(valueStrMap);
		} catch (Exception e) {
			log.warn(e.getMessage());
			return "";
		}
	}


	public static Map<String,String> readAppForceVersion(String os, RequestEntity requestEntity,
													CommonReadOperation operation, JedisUtils jedisUtils,boolean cachedValue) throws Exception{
		return readData(os, Constants.FORCE_UPDATE_VERSION, Constants.CACHED_KEY_APP_VERSION_INFO_FIELD, requestEntity, operation, jedisUtils, cachedValue);
	}
}

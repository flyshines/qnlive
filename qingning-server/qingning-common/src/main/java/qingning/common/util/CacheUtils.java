package qingning.common.util;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import qingning.common.entity.RequestEntity;
import qingning.server.JedisBatchCallback;
import qingning.server.JedisBatchOperation;
import qingning.server.rpc.CommonReadOperation;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;

public final class CacheUtils {
	private static ObjectMapper objectMapper = new ObjectMapper();
	@SuppressWarnings({"rawtypes" })
	private static Map<String,String> readData(String searchKey, String keyFormat, String keyField, 
			RequestEntity requestEntity, CommonReadOperation operation, JedisUtils jedisUtils, boolean cachedValue) throws Exception{
		boolean useCached = jedisUtils!=null;
		Map<String,String> dataValue = null;
		Map<String, String> keyMap = new HashMap<String, String>();
		keyMap.put(keyField, searchKey);
		String key = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_USER, keyMap);
		Jedis jedis = null;
		if(useCached){
			jedis = jedisUtils.getJedis();
			dataValue=jedis.hgetAll(key);
		}
		
		if(MiscUtils.isEmpty(dataValue)){
			Map result = (Map) operation.invoke(requestEntity);
			if(!MiscUtils.isEmpty(dataValue) && cachedValue){
				dataValue = new HashMap<String, String>();
				for(Object curKey:result.keySet()){
					dataValue.put((String)curKey,MiscUtils.convertString(result.get(curKey)));
				}
				if(useCached){
					jedis.hmset(key, dataValue);
				}
			}
		}
		return dataValue;
	}
	
	@SuppressWarnings({"rawtypes" })
	private static List<Map<String,String>> readListFromDB(String keyFormat, String listKey, String sortKey, String primaryKey, RequestEntity requestEntity, 
			CommonReadOperation operation, JedisUtils jedisUtils, boolean cachedList) throws Exception{
		boolean useCached = jedisUtils!=null && cachedList;		
		List result = (List) operation.invoke(requestEntity);
		
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
	
	
	
	public static Map<String,String> readUser(String userId, RequestEntity requestEntity, 
			CommonReadOperation operation, JedisUtils jedisUtils) throws Exception{
		return readData(userId, Constants.CACHED_KEY_USER, Constants.CACHED_KEY_USER_FIELD, requestEntity, operation, jedisUtils, true);
	}
	
	public static Map<String,String> readLecturer(String lecturerId, RequestEntity requestEntity, 
			CommonReadOperation operation, JedisUtils jedisUtils) throws Exception{					
		Map<String,String> values = readData(lecturerId, Constants.CACHED_KEY_LECTURER, Constants.CACHED_KEY_LECTURER_FIELD, requestEntity, operation, jedisUtils,true);
		if(jedisUtils != null){
			Jedis jedis = jedisUtils.getJedis();			
			Map<String, String> keyMap = new HashMap<String, String>();
			keyMap.put(Constants.CACHED_KEY_LECTURER_FIELD, lecturerId);
			String key = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER_ROOMS, keyMap);
			if(!jedis.exists(key)){
				readListFromDB(Constants.CACHED_KEY_SPECIAL_LECTURER_ROOM, key, Constants.FIELD_CREATE_TIME, Constants.FIELD_ROOM_ID,requestEntity, operation, jedisUtils, true);				
			}
		}
		return values;
	}
	
	public static Map<String,String> readCourse(String course_id, RequestEntity requestEntity, 
			CommonReadOperation operation, JedisUtils jedisUtils) throws Exception{
		return readData(course_id, Constants.CACHED_KEY_COURSE, Constants.CACHED_KEY_COURSE_FIELD, requestEntity, operation, jedisUtils, false);
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
				list = (List<Map<String,Object>>)operation.invoke(requestEntity);
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
				list = (List<Map<String,Object>>)operation.invoke(requestEntity);
				if(MiscUtils.isEmpty(list)){
					list = new LinkedList<Map<String,Object>>();
				}
				audiosJsonList = objectMapper.writeValueAsString(list);
			}
			values.put(Constants.CACHED_KEY_COURSE_PPTS_FIELD, audiosJsonList);
		}
		return values;
	}
}

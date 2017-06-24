package qingning.common.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qingning.common.entity.QNLiveException;
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
			RequestEntity requestEntity, CommonReadOperation operation, Jedis jedis, boolean cachedValue) throws Exception{
		return readData(searchKey, keyFormat, keyField, requestEntity, operation, jedis, cachedValue, -1);
	}

	private static Map<String,String> readData(String searchKey, String keyFormat, String keyField, 
			RequestEntity requestEntity, CommonReadOperation operation, Jedis jedis, boolean cachedValue, int lifeTime) throws Exception{
		String[] searchKeys={searchKey};
		String[] keyFields={keyField};
		return readData(searchKeys, keyFormat, keyFields, requestEntity, operation, jedis, cachedValue, lifeTime);
	}
	
	@SuppressWarnings({"rawtypes", "unchecked" })
	private static Map<String,String> readData(String[] searchKeys, String keyFormat, String[] keyFields, 
			RequestEntity requestEntity, CommonReadOperation operation, Jedis jedis, boolean cachedValue, int lifeTime) throws Exception{
		Map<String,String> dataValue = null;
		Map<String, String> keyMap = new HashMap<String, String>();
		int length = searchKeys.length;
		for(int i = 0; i < length; ++i){
			keyMap.put(keyFields[i], searchKeys[i]);
		}		
		String key = MiscUtils.getKeyOfCachedData(keyFormat, keyMap);
		dataValue=jedis.hgetAll(key);

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
			CommonReadOperation operation, Jedis jedis) throws Exception{
		Map<String,String> result = readData(userId, Constants.CACHED_KEY_USER, Constants.CACHED_KEY_USER_FIELD, requestEntity, operation, jedis, false, -1);
		return result;
	}
	
	@SuppressWarnings("unchecked")
	public static Map<String,String> readUser(String userId, RequestEntity requestEntity,
			CommonReadOperation operation, Jedis jedis) throws Exception{
		Map<String,String> result = readData(userId, Constants.CACHED_KEY_USER, Constants.CACHED_KEY_USER_FIELD, requestEntity, operation, jedis, true, 60*60*72);
		if(!MiscUtils.isEmpty(result)){
			Map<String,Object> query = new HashMap<String,Object>();
			query.put(Constants.CACHED_KEY_USER_FIELD, userId);
			//<editor-fold desc="用户加入的课程">
			final String coursesKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_USER_COURSES, query);
			if(jedis.exists(coursesKey)){
				jedis.expire(coursesKey, 60*60*72);
			} else {
				RequestEntity entity = new RequestEntity();
				entity.setFunctionName(Constants.SYS_READ_USER_COURSE_LIST);
				entity.setParam(query);
				query.put("size", Constants.MAX_QUERY_LIMIT);	
				final Set<String> userCourseSet = new HashSet<String>();
				int readCount = 0;
				long student_pos=0;
				do{
					if(student_pos>0){
						query.put("student_pos", student_pos);
					}
					List<Map<String,Object>> list = (List<Map<String,Object>>)operation.invokeProcess(entity);
					if(!MiscUtils.isEmpty(list)){
						readCount = list.size();
						for(Map<String,Object> course:list){
							if(course.get("status").equals("2") ||course.get("status").equals("1") ){
								userCourseSet.add((String)course.get("course_id"));
								student_pos = MiscUtils.convertObjectToLong(course.get("student_pos"));
							}else{
								readCount--;
							}
						}
					} else {
						readCount = 0;
					}
				} while(readCount==Constants.MAX_QUERY_LIMIT);
				if(!MiscUtils.isEmpty(userCourseSet)){
					((JedisBatchCallback)jedis).invoke(new JedisBatchOperation(){
						@Override
						public void batchOperation(Pipeline pipeline, Jedis jedis) {
							for(String courseId:userCourseSet){
								pipeline.sadd(coursesKey, courseId);
							}
							pipeline.sync();
						}						
					});
					jedis.expire(coursesKey, 60*60*72);
				}
			}
			//</editor-fold>

			//<editor-fold desc="用户关注直播间">
			final String roomsKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_USER_ROOMS,query);//存储用户关注的直播间
			if(jedis.exists(roomsKey)){//判断当前缓存是否存在
				jedis.expire(roomsKey, 60*60*72);//修改生命时间
			} else {
				RequestEntity entity = new RequestEntity();
				entity.setFunctionName(Constants.SYS_READ_USER_ROOM_LIST);//加入参数 进行方法调用 获取房间id list
				entity.setParam(query);
				final Set<String> userRoomSet = new HashSet<String>();
				List<Map<String, Object>> list = (List<Map<String, Object>>) operation.invokeProcess(entity);//调用传入的operation对象里的invokeProcess方法
				if (!MiscUtils.isEmpty(list)) {
					for (Map<String, Object> course : list) {
						userRoomSet.add((String) course.get("room_id"));
					}
				}
				if (!MiscUtils.isEmpty(userRoomSet)) {
					((JedisBatchCallback) jedis).invoke(new JedisBatchOperation() {
						@Override
						public void batchOperation(Pipeline pipeline, Jedis jedis) {
							for (String roomId : userRoomSet) {
								pipeline.sadd(roomsKey, roomId);
							}
							pipeline.sync();
						}
					});
					jedis.expire(roomsKey, 60 * 60 * 72);
				}
			}
			//</editor-fold>

			String userCacheKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_USER, query);//获取系统缓存key
			Long course_num = jedis.scard(MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_USER_COURSES, query));//课程存储key 获取加入课程总数
			if(course_num != Long.parseLong(result.get("course_num"))){
				jedis.hset(userCacheKey,"course_num",course_num.toString());//修改用户缓存中的数据
			}

			Long room_num = jedis.scard(MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_USER_ROOMS, query));//查询用户关注直播间总数
			if(room_num != Long.parseLong(result.get("live_room_num"))){ //如果数据和现在的不同
				jedis.hset(userCacheKey,"live_room_num",room_num.toString());//修改用户缓存中的数据
			}
			result = readData(userId, Constants.CACHED_KEY_USER, Constants.CACHED_KEY_USER_FIELD, requestEntity, operation, jedis, true, 60*60*72);
		}
		return result;
	}
	
	public static Map<String,String> readLecturer(String lecturerId, RequestEntity requestEntity,
			CommonReadOperation operation, Jedis jedis) throws Exception{
		Map<String,String> values = readData(lecturerId, Constants.CACHED_KEY_LECTURER, Constants.CACHED_KEY_LECTURER_FIELD, requestEntity, operation, jedis,true);
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
	
	public static Map<String,String> readCourse(String course_id, RequestEntity requestEntity, CommonReadOperation operation,Jedis jedis,boolean cachedValue) throws Exception{
		Map<String,String> values = readData(course_id, Constants.CACHED_KEY_COURSE, Constants.CACHED_KEY_COURSE_FIELD, requestEntity, operation, jedis, cachedValue);
		String curCourse_id = values.get(Constants.CACHED_KEY_COURSE_FIELD);
		if(!course_id.equals(curCourse_id)){
			Map<String, String> keyMap = new HashMap<String, String>();
			keyMap.put(Constants.CACHED_KEY_COURSE_FIELD, course_id);
			String key = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE, keyMap);
			jedis.del(key);
			values = readData(course_id, Constants.CACHED_KEY_COURSE, Constants.CACHED_KEY_COURSE_FIELD, requestEntity, operation, jedis, cachedValue);
		}
		return values;
	}

	public static Map<String,String> readSeries(String series_id, RequestEntity requestEntity, CommonReadOperation operation,Jedis jedis,boolean cachedValue) throws Exception{
		Map<String,String> values = readData(series_id, Constants.CACHED_KEY_SERIES, Constants.CACHED_KEY_SERIES_FIELD, requestEntity, operation, jedis, cachedValue);
		String seriesId = values.get(Constants.CACHED_KEY_SERIES_FIELD);
		if(!series_id.equals(seriesId)){
			Map<String, String> keyMap = new HashMap<String, String>();
			keyMap.put(Constants.CACHED_KEY_SERIES_FIELD, series_id);
			String key = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_SERIES, keyMap);
			jedis.del(key);
			values = readData(series_id, Constants.CACHED_KEY_SERIES, Constants.CACHED_KEY_SERIES_FIELD, requestEntity, operation, jedis, cachedValue);
		}
		return values;
	}
	
	/**
	 * 获取讲师已上架的所有系列课程id列表
	 * @param lecturerId 讲师id
	 * @param lastSeriesId 上一页最后一条数据的系列id
	 * @param pageCount 每页数量
	 * @param jedis
	 * @return
	 */
	public static Set<String> readLecturerSeriesUp(String lecturerId, String lastSeriesId, int pageCount, Jedis jedis){
		//返回结果集
		Set<String> seriesSet = null;
		
		Map<String, Object> keyMap = new HashMap<>();
        keyMap.put(Constants.CACHED_KEY_LECTURER_FIELD, lecturerId);
        String lecturerSeriesSetKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER_SERIES_UP, keyMap);
        if(jedis.exists(lecturerSeriesSetKey)){	//缓存中存在
        	//获取上一页最后一条数据的score
            if(lastSeriesId != null && !"0".equals(lastSeriesId)){	//不是获取第一页数据
    	        Double lastScore = jedis.zscore(lecturerSeriesSetKey, lastSeriesId);
    	        //分页获取系列课程中的id列表
    	        seriesSet = jedis.zrangeByScore(lecturerSeriesSetKey, "(" + lastScore, "+inf", 0, pageCount);
            }else{	//获取第一页数据
    	        //分页获取系列课程中的id列表
    	        seriesSet = jedis.zrangeByScore(lecturerSeriesSetKey, "-inf", "+inf", 0, pageCount);
            }
        }
        
		return seriesSet;
	}



	@SuppressWarnings("unchecked")
	public static Map<String,String> readFullCourseInfo(String course_id, RequestEntity requestEntity, 
			CommonReadOperation operation, Jedis jedis) throws Exception{
		Map<String,String> values = readData(course_id, Constants.CACHED_KEY_COURSE, Constants.CACHED_KEY_COURSE_FIELD, requestEntity, operation, jedis, false);
		if(!values.containsKey(Constants.CACHED_KEY_COURSE_PPTS_FIELD)){
			String pptJsonList = null;
			Map<String, String> keyMap = new HashMap<String, String>();
			keyMap.put(Constants.CACHED_KEY_COURSE_FIELD, course_id);
			String key = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_PPTS, keyMap);
			pptJsonList = jedis.get(key);
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
			Map<String, String> keyMap = new HashMap<String, String>();
			keyMap.put(Constants.CACHED_KEY_COURSE_AUDIOS, course_id);
			String key = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_AUDIOS, keyMap);
			audiosJsonList = jedis.get(key);
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
			CommonReadOperation operation, Jedis jedis,boolean cachedValue) throws Exception{
		return readLiveRoom(room_id, requestEntity, operation, jedis, cachedValue, false);
	}
	
	public static Map<String,String> readLiveRoom(String room_id, RequestEntity requestEntity,
												CommonReadOperation operation, Jedis jedis,boolean cachedValue,boolean init) throws Exception{
		Map<String,String> values =  readData(room_id, Constants.CACHED_KEY_ROOM, Constants.FIELD_ROOM_ID, requestEntity, operation, jedis, cachedValue);
		if(init && !MiscUtils.isEmpty(values) && cachedValue){
	        Map<String, Object> map = new HashMap<String, Object>();
	        map.put(Constants.CACHED_KEY_LECTURER_FIELD, values.get(Constants.CACHED_KEY_LECTURER_FIELD));
	        String lectureLiveRoomKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER_ROOMS, map);
			jedis.hset(lectureLiveRoomKey, room_id, "1");
		}
		return values;
	}
	
	public static Map<String,String> readRoomDistributerDetails(String room_id, String distributer_id, String rq_code, RequestEntity requestEntity,
			CommonReadOperation operation, Jedis jedis) throws Exception{
		String[] searchKeys={room_id,distributer_id,rq_code};
		String[] keyFields={"room_id","distributer_id","rq_code"};
		return readData(searchKeys, Constants.CACHED_KEY_USER_DISTRIBUTERS_ROOM_RQ, keyFields, requestEntity, operation, jedis, true, 60*1000);
	}
	
	public static String readLiveRoomInfoFromCached(String room_id, String fieldName,RequestEntity requestEntity,
			CommonReadOperation operation,Jedis jedis,boolean cachedValue) throws Exception{
		if(MiscUtils.isEmpty(fieldName)){
			return "";
		}
		
		Map<String, String> keyMap = new HashMap<String, String>();
		keyMap.put(Constants.FIELD_ROOM_ID, room_id);
		String key = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_ROOM, keyMap);
		String result = jedis.hget(key, fieldName);
		if(MiscUtils.isEmpty(result) && !jedis.exists(key)){
			Map<String,String> values = readLiveRoom(room_id,requestEntity,operation,jedis,cachedValue);
			if(!MiscUtils.isEmpty(values)){
				result = values.get(fieldName);
			}
		}
		return result;
	}
	
	public static Map<String,String> readDistributer(String distributer_id, RequestEntity requestEntity,
			CommonReadOperation operation, Jedis jedis,boolean cachedValue) throws Exception{
		return readData(distributer_id, Constants.CACHED_KEY_DISTRIBUTER, Constants.CACHED_KEY_DISTRIBUTER_FIELD, requestEntity, operation, jedis, cachedValue);
	}

	public static Map<String,String> readDistributerRoom(String distributer_id, String room_id, CommonReadOperation operation,  Jedis jedis) throws Exception{
		return readDistributerRoom(distributer_id,room_id,operation,jedis,false);
	}
	
	public static Map<String,String> readDistributerRoom(String distributer_id, String room_id, CommonReadOperation operation, Jedis jedis, boolean expired) throws Exception{
		String[] searchKeys={distributer_id,room_id};
		String[] keyFields={Constants.CACHED_KEY_DISTRIBUTER_FIELD,Constants.FIELD_ROOM_ID};
		RequestEntity requestEntity = new RequestEntity();
		int len = searchKeys.length;

		Map<String,Object> query = new HashMap<>();
		for(int i=0; i<len; ++i){
			query.put(keyFields[i], searchKeys[i]);
		}
		Date currentDate = new Date();
		if(!expired){
			query.put("current_date", currentDate);
		} else {
			query.remove("current_date");
		}
		requestEntity.setParam(query);
		Map<String,String> values =  readData(searchKeys, Constants.CACHED_KEY_ROOM_DISTRIBUTER, keyFields, requestEntity, operation, jedis, true, -1);
		if(!expired && !MiscUtils.isEmpty(values)){
			String end_date = values.get("end_date");
			if(!MiscUtils.isEmpty(end_date)){
				long endDate = MiscUtils.convertObjectToLong(end_date);
				if(currentDate.getTime() > endDate){					
					//String key = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_ROOM_DISTRIBUTER, query);
			        //String oldRQcode = (String)values.get("rq_code");
			        //Map<String,Object> queryParam = new HashMap<>();
			        //queryParam.put(Constants.CACHED_KEY_ROOM_DISTRIBUTER_RQ_CODE_FIELD, oldRQcode);
			        //String oldRQcodeKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_ROOM_DISTRIBUTER_RQ_CODE, queryParam);			        
			        //jedisUtils.getJedis().del(key,oldRQcodeKey);
			        values=null;
				}
			}
		}
		return values;
	}
	
	public static Map<String,String> readAppVersion(String os, RequestEntity requestEntity,
													 CommonReadOperation operation, Jedis jedis,boolean cachedValue) throws Exception{
		return readData(os, Constants.CACHED_KEY_APP_VERSION_INFO, Constants.CACHED_KEY_APP_VERSION_INFO_FIELD, requestEntity, operation, jedis, cachedValue,60*60);
	}

	
	public static List<Map<String,String>> readCourseListInfoOnlyFromCached(Jedis jedis, List<String> courseIdList,
			CommonReadOperation operation){
		final List<Map<String,String>> result = new ArrayList<Map<String,String>>();
		if(MiscUtils.isEmpty(courseIdList)){
			return result;
		}
		JedisBatchCallback callBack = (JedisBatchCallback)jedis;
		
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
													CommonReadOperation operation, Jedis jedis,boolean cachedValue) throws Exception{
		return readData(os, Constants.FORCE_UPDATE_VERSION, Constants.CACHED_KEY_APP_VERSION_INFO_FIELD, requestEntity, operation, jedis, cachedValue);
	}
	
	public static Map<String,String> readLastCourseOfTheRoom(String roomId, String lectureId,RequestEntity requestEntity, CommonReadOperation operation, Jedis jedis) throws Exception{
        Map<String,Object> map = new HashMap<String,Object>();
        map.put(Constants.CACHED_KEY_LECTURER_FIELD, lectureId);
        String lecturerCoursesFinishKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_FINISH, map);
        Set<String> courseSet = jedis.zrevrangeByScore(lecturerCoursesFinishKey, "+inf", "-inf", 0, 1);
        String courseId = null;
        if(courseSet != null){
        	for(String value:courseSet){
        		courseId = value;
        	}
        }
        Map<String,String> courseInfo = new HashMap<String,String>();
        if(MiscUtils.isEmpty(courseId)){
        	Map result = (Map) operation.invokeProcess(requestEntity);
        	if(!MiscUtils.isEmpty(result)){
        		MiscUtils.converObjectMapToStringMap(result, courseInfo);
        	}
        	if(!MiscUtils.isEmpty(courseInfo)){
        		Map<String, String> keyMap = new HashMap<String, String>();
    			keyMap.put(Constants.CACHED_KEY_COURSE_FIELD, (String)courseInfo.get("course_id"));
    			String key = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE, keyMap);
    			jedis.hmset(key, courseInfo);
        	} else {
        		/*courseSet = jedis.zrangeByScore(lecturerCoursesPredictionKey, "-inf", "+inf", 0, 1);
            	for(String value:courseSet){
            		courseId = value;
            	}*/
        	}
        }
        if(MiscUtils.isEmpty(courseInfo) && !MiscUtils.isEmpty(courseId)){
    		Map<String, String> keyMap = new HashMap<String, String>();
			keyMap.put(Constants.CACHED_KEY_COURSE_FIELD, courseId);
			String key = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE, keyMap);
			courseInfo = jedis.hgetAll(key);
        }
        return courseInfo;
	}


	public static Map<String,String> readShop(String shop_id, RequestEntity requestEntity, CommonReadOperation operation,Jedis jedis) throws Exception{
		Map<String,String> values = readData(shop_id, Constants.CACHED_KEY_SHOP, Constants.CACHED_KEY_SHOP_FIELD, requestEntity, operation, jedis, true);
		if(values.isEmpty()){
			//店铺不存在
			throw new QNLiveException("190001");
		}
		String curShop_id = values.get(Constants.CACHED_KEY_SHOP_FIELD);
		if(!shop_id.equals(curShop_id)){
			Map<String, String> keyMap = new HashMap<String, String>();
			keyMap.put(Constants.CACHED_KEY_SHOP_FIELD, shop_id);
			String key = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_SHOP, keyMap);
			jedis.del(key);
			values = readData(shop_id, Constants.CACHED_KEY_SHOP, Constants.CACHED_KEY_SHOP_FIELD, requestEntity, operation, jedis, true);
		}
		return values;
	}

	public static Map<String,String> readShopByUserId(String user_id, RequestEntity requestEntity, CommonReadOperation operation,Jedis jedis) throws Exception{
		Map<String,String> values = readData(user_id, Constants.CACHED_KEY_SHOP, Constants.CACHED_KEY_SHOP_FIELD, requestEntity, operation, jedis, false);
		if(!values.isEmpty()){
			String curShop_id = values.get(Constants.CACHED_KEY_SHOP_FIELD);

			String[] searchKeys={curShop_id};
			String[] keyFields={Constants.CACHED_KEY_SHOP_FIELD};

			Map<String, String> keyMap = new HashMap<String, String>();
			int length = searchKeys.length;
			for(int i = 0; i < length; ++i){
				keyMap.put(keyFields[i], searchKeys[i]);
			}
			String key = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_SHOP, keyMap);
			jedis.hmset(key, values);
		}else{
			//店铺不存在
			throw new QNLiveException("190001");
		}
		return values;
	}

	/**
	 * 获取讲师已上架的所有单品课程（直播除外）id列表
	 * @param lecturerId 讲师id
	 * @param lastSingleId 上一页最后一条数据的单品课程id
	 * @param pageCount 每页数量
	 * @param jedis
	 * @return
	 */
	public static Set<String> readLecturerSingleNotLiveUp(String lecturerId, String lastSingleId, int pageCount,RequestEntity requestEntity,CommonReadOperation operation,
			Jedis jedis) {
		//返回结果集
		Set<String> singleSet = null;
		
		Map<String, Object> keyMap = new HashMap<>();
        keyMap.put(Constants.CACHED_KEY_LECTURER_FIELD, lecturerId);
        String lecturerSingleSetKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER_COURSES_NOT_LIVE_UP, keyMap);
        if(jedis.exists(lecturerSingleSetKey)){	//缓存中存在
        	//获取上一页最后一条数据的score
            if(lastSingleId != null && !"0".equals(lastSingleId)){	//不是获取第一页数据
    	        Double lastScore = jedis.zscore(lecturerSingleSetKey, lastSingleId);
    	        //分页获取单品课程中的id列表
    	        singleSet = jedis.zrangeByScore(lecturerSingleSetKey, "(" + lastScore, "+inf", 0, pageCount);
            }else{	//获取第一页数据
    	        //分页获取单品课程中的id列表
            	singleSet = jedis.zrangeByScore(lecturerSingleSetKey, "-inf", "+inf", 0, pageCount);
            }
        }else{
			//缓存不存在，更新缓存
			try {
				Object obj = operation.invokeProcess(requestEntity);
				if(obj!=null){
					List<String> list = (List)obj;
					long timeStamp = System.currentTimeMillis();
					//按数据库返回的数据进行排序
					for(String courseId:list){
						jedis.zadd(lecturerSingleSetKey,timeStamp++,courseId);
					}
				}else{
					//未查到课程列表
					return null;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return singleSet;
	}
	
}

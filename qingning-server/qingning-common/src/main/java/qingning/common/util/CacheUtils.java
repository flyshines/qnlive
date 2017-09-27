package qingning.common.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import qingning.common.entity.QNLiveException;
import qingning.common.entity.RequestEntity;
import qingning.server.JedisBatchCallback;
import qingning.server.JedisBatchOperation;
import qingning.server.JedisServer;
import qingning.server.rpc.CommonReadOperation;
import qingning.server.rpc.initcache.*;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;
import redis.clients.jedis.Tuple;

import java.util.*;

public class CacheUtils extends JedisServer{
	protected ReadUserOperation readUserOperation;
	protected ReadShopOperation readShopOperation;
	protected ReadCourseOperation readCourseOperation;
	protected ReadSeriesOperation readSeriesOperation;
	protected ReadVersionOperation readAPPVersionOperation;
	protected ReadVersionForceOperation readForceVersionOperation;
	protected ReadLecturerOperation readLecturerOperation;

	protected JedisUtils jedisUtils;

	private static Logger log = LoggerFactory.getLogger(CacheUtils.class);
	private static ObjectMapper objectMapper = new ObjectMapper();
	
	private Map<String,String> readData(String searchKey, String keyFormat, String keyField,
			RequestEntity requestEntity, CommonReadOperation operation, Jedis jedis, boolean cachedValue) throws Exception{
		return readData(searchKey, keyFormat, keyField, requestEntity, operation, jedis, cachedValue, -1);
	}

	private Map<String,String> readDataByFunction(String searchKey, String keyFormat, String keyField,
			Map<String,Object> param, String functionName,CommonReadOperation operation, Jedis jedis, boolean cachedValue) throws Exception{
		String[] searchKeys={searchKey};
		String[] keyFields={keyField};
		return readData(searchKeys, keyFormat, keyFields, param,functionName, operation, jedis, cachedValue, -1);
	}

	private Map<String,String> readData(String searchKey, String keyFormat, String keyField,
			RequestEntity requestEntity, CommonReadOperation operation, Jedis jedis, boolean cachedValue, int lifeTime) throws Exception{
		String[] searchKeys={searchKey};
		String[] keyFields={keyField};
		return readData(searchKeys, keyFormat, keyFields, requestEntity, operation, jedis, cachedValue, lifeTime);
	}
	
	@SuppressWarnings({"rawtypes", "unchecked" })
	private Map<String,String> readData(String[] searchKeys, String keyFormat, String[] keyFields,
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
				dataValue = new HashMap<>();
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
	private Map<String,String> readData(String[] searchKeys, String keyFormat, String[] keyFields,
			Map<String,Object> param,String functionName,CommonReadOperation operation, Jedis jedis, boolean cachedValue, int lifeTime) throws Exception{
		Map<String,String> dataValue;
		Map<String, String> keyMap = new HashMap<>();
		int length = searchKeys.length;
		for(int i = 0; i < length; ++i){
			keyMap.put(keyFields[i], searchKeys[i]);
		}
		String key = MiscUtils.getKeyOfCachedData(keyFormat, keyMap);
		dataValue=jedis.hgetAll(key);

		if(MiscUtils.isEmpty(dataValue)){
			Map result = (Map) operation.invokeProcessByFunction(param,functionName);
			if(!MiscUtils.isEmpty(result)){
				dataValue = new HashMap<>();
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

	public Map<String,String> readUser(String userId, RequestEntity requestEntity,
			CommonReadOperation operation,Jedis jedis) throws Exception{
		Map<String,String> result = readData(userId, Constants.CACHED_KEY_USER, Constants.CACHED_KEY_USER_FIELD, requestEntity, operation, jedis, true, 60*60*72);
		if(MiscUtils.isEmpty(result)){
			throw new QNLiveException("000005");
		}
		return result;
	}
	
	public Map<String,String> readLecturer(String lecturerId, RequestEntity requestEntity,
			CommonReadOperation operation, Jedis jedis) throws Exception{
		Map<String,String> values = readData(lecturerId, Constants.CACHED_KEY_LECTURER, Constants.CACHED_KEY_LECTURER_FIELD, requestEntity, operation, jedis,true);
		return values;
	}
	
	public Map<String,String> readCourse(String course_id, RequestEntity requestEntity, CommonReadOperation operation,Jedis jedis,boolean cachedValue) throws Exception{
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
	public Map<String,String> readSeries(String series_id, Map<String,Object> param, String functionName, Jedis jedis,boolean cachedValue) throws Exception{
		Map<String,String> values = readDataByFunction(series_id, Constants.CACHED_KEY_SERIES, Constants.CACHED_KEY_SERIES_FIELD, param,functionName, readSeriesOperation, jedis, cachedValue);
		String seriesId = values.get(Constants.CACHED_KEY_SERIES_FIELD);
		if(!series_id.equals(seriesId)){
			Map<String, String> keyMap = new HashMap<String, String>();
			keyMap.put(Constants.CACHED_KEY_SERIES_FIELD, series_id);
			String key = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_SERIES, keyMap);
			jedis.del(key);
			values = readDataByFunction(series_id, Constants.CACHED_KEY_SERIES, Constants.CACHED_KEY_SERIES_FIELD,  param,functionName, readSeriesOperation, jedis, cachedValue);
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


	public Map<String,String> readAppVersion(String os, RequestEntity requestEntity,
													 CommonReadOperation operation, Jedis jedis,boolean cachedValue) throws Exception{
		return readData(os, Constants.CACHED_KEY_APP_VERSION_INFO, Constants.CACHED_KEY_APP_VERSION_INFO_FIELD, requestEntity, operation, jedis, cachedValue,60*60);
	}


	public static Map<String,Object> convertCachedStringToMap(String valueStr){
		if(MiscUtils.isEmptyString(valueStr)) return null;
		Map<String, Object> value;
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
			Map<String,String> valueStrMap = new HashMap<>();
			MiscUtils.converObjectMapToStringMap(values, valueStrMap);
			return objectMapper.writeValueAsString(valueStrMap);
		} catch (Exception e) {
			log.warn(e.getMessage());
			return "";
		}
	}


	public Map<String,String> readAppForceVersion(String os, RequestEntity requestEntity,
													CommonReadOperation operation, Jedis jedis,boolean cachedValue) throws Exception{
		return readData(os, Constants.FORCE_UPDATE_VERSION, Constants.CACHED_KEY_APP_VERSION_INFO_FIELD, requestEntity, operation, jedis, cachedValue);
	}
	

	public Map<String,String> readShop(String shop_id, Map<String,Object> param,String functionName,Boolean force,Jedis jedis) throws Exception{
		Map<String,String> values = readDataByFunction(shop_id, Constants.CACHED_KEY_SHOP, Constants.CACHED_KEY_SHOP_FIELD, param , functionName, readShopOperation, jedis, force);

		if(values.isEmpty()){
			//店铺不存在
			throw new QNLiveException("190001");
		}
		String curShop_id = values.get(Constants.CACHED_KEY_SHOP_FIELD);
		if(!shop_id.equals(curShop_id)){
			Map<String, String> keyMap = new HashMap<>();
			keyMap.put(Constants.CACHED_KEY_SHOP_FIELD, shop_id);
			String key = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_SHOP, keyMap);
			jedis.del(key);
			values = readDataByFunction(shop_id, Constants.CACHED_KEY_SHOP, Constants.CACHED_KEY_SHOP_FIELD,  param , functionName, readShopOperation, jedis, true);
		}
		return values;
	}

	/**获取用户accessToken String信息
	 * @param accessToken
	 * @param key
	 */
	public String getAccessInfoByToken(String accessToken,String key,Jedis jedis){
		Map<String,String> map = new HashMap<>();
		map.put(Constants.CACHED_KEY_ACCESS_TOKEN_FIELD, accessToken);
		String accessTokenKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_ACCESS_TOKEN, map);
		return jedis.hget(accessTokenKey, key);
	}

	/**获取用户accessToken HASH信息
	 * @param accessToken
	 */
	public Map<String, String> getAccessInfoByToken(String accessToken,Jedis jedis){
		Map<String,String> map = new HashMap<>();
		map.put(Constants.CACHED_KEY_ACCESS_TOKEN_FIELD, accessToken);
		String accessTokenKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_ACCESS_TOKEN, map);
		return jedis.hgetAll(accessTokenKey);
	}

	/**更新用户accessToken信息
	 * @param accessToken
	 * @param key
	 * @param value
	 */
	public void updateAccessInfoByToken(String accessToken,String key,String value,Jedis jedis){

		Map<String,String> map = new HashMap<>();
		map.put(Constants.CACHED_KEY_ACCESS_TOKEN_FIELD, accessToken);
		String accessTokenKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_ACCESS_TOKEN, map);
		jedis.hset(accessTokenKey,key,value);
	}

	/**更新用户accessToken信息
	 * @param accessToken
	 * @param param
	 * @return
	 * @throws Exception
	 */
	public void updateAccessInfoByToken(String accessToken,Map<String,String> param,Jedis jedis) throws Exception{

		Map<String,String> map = new HashMap<>();
		map.put(Constants.CACHED_KEY_ACCESS_TOKEN_FIELD, accessToken);
		String accessTokenKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_ACCESS_TOKEN, map);
		for(String key:param.keySet()){
			jedis.hset(accessTokenKey,key,param.get(key));
		}
	}

	public Map<String,String> readCurrentUserShop(RequestEntity requestEntity, Jedis jedis) throws Exception{
		String shopId = this.getAccessInfoByToken(requestEntity.getAccessToken(),Constants.CACHED_KEY_SHOP_FIELD,jedis);
		Map<String,Object> param = new HashMap<>();
		param.put("shop_id",shopId);
		Map<String,String> values = readDataByFunction(shopId, Constants.CACHED_KEY_SHOP, Constants.CACHED_KEY_SHOP_FIELD, param,CommonReadOperation.CACHE_READ_SHOP, readShopOperation, jedis, false);
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
	 * @return
	 */
	public Set<String> readLecturerSingleNotLiveUp(String lecturerId, String lastSingleId, int pageCount,RequestEntity requestEntity,CommonReadOperation operation,Jedis jedis)  throws Exception{

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
    	        singleSet = jedis.zrevrangeByScore(lecturerSingleSetKey, "(" + lastScore, "-inf", 0, pageCount);
            }else{	//获取第一页数据
    	        //分页获取单品课程中的id列表
            	singleSet = jedis.zrevrangeByScore(lecturerSingleSetKey, "+inf", "-inf", 0, pageCount);
            }
        }else{
			//缓存不存在，更新缓存
			Object obj = operation.invokeProcess(requestEntity);
			if(obj!=null){
				List<String> list = (List)obj;
				long timeStamp = System.currentTimeMillis();
				//按数据库返回的数据进行排序
				for(String courseId:list){
					jedis.zadd(lecturerSingleSetKey,timeStamp--,courseId);
				}
			}else{
				//未查到课程列表
				return null;
			}
		}
		return singleSet;
	}

	/**
	 * 读取系列的子课程id列表
	 * @param seriesId 系列id
	 * @param lastCourseId 上一页最后一条数据的课程id
	 * @param pageCount 每页数量
	 * @return
	 */
	public Set<String> readSeriesCourseUp(String seriesId, String lastCourseId, int pageCount,Jedis jedis)
			throws Exception{
		//返回结果集
		Set<String> courseSet = null;
		
		Map<String, Object> keyMap = new HashMap<>();
        keyMap.put(Constants.CACHED_KEY_SERIES_FIELD, seriesId);
        String seriesCourseSetKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_SERIES_COURSE_UP, keyMap);
        if(jedis.exists(seriesCourseSetKey)){	//缓存中存在
        	//获取上一页最后一条数据的score
            if(lastCourseId != null && !"0".equals(lastCourseId)){	//不是获取第一页数据
    	        Double lastScore = jedis.zscore(seriesCourseSetKey, lastCourseId);
    	        //分页获取系列的课程的id列表
    	        courseSet = jedis.zrangeByScore(seriesCourseSetKey, "(" + lastScore, "+inf", 0, pageCount);
            }else{	//获取第一页数据
    	        //分页获取系列的课程的id列表
            	courseSet = jedis.zrangeByScore(seriesCourseSetKey, "-inf", "+inf", 0, pageCount);
            }
        }
		return courseSet;
	}

	/**
	 * 读取saas课程的留言id排序列表
	 * @param courseId 课程id
	 * @param lastMessageId 上一页最后一条数据的留言id
	 * @param pageCount 每页数量
	 * @return 以创建时间倒序获取
	 * @throws Exception
	 */
	public Set<String> readCourseMessageSet(String courseId, String lastMessageId, int pageCount,Jedis jedis)
			throws Exception{
		//返回结果集
		Set<String> messageSet = null;
		
		Map<String, Object> keyMap = new HashMap<>();
        keyMap.put(Constants.CACHED_KEY_COURSE_FIELD, courseId);
        String messageSetKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_SAAS_COMMENT_ALL, keyMap);
        if(jedis.exists(messageSetKey)){	//缓存中存在
        	//获取上一页最后一条数据的score
            if(lastMessageId != null && !"0".equals(lastMessageId)){	//不是获取第一页数据
    	        Double lastScore = jedis.zscore(messageSetKey, lastMessageId);
    	        /*
    	         * 分页获取单品课程中的id列表
    	         * 倒序获取：因为缓存中是按创建时间递增排序，而需求是按照创建时间递减
    	         */
    	        messageSet = jedis.zrevrangeByScore(messageSetKey, "("+lastScore, "-inf", 0, pageCount);
            }else{	//获取第一页数据
    	        /*
    	         * 分页获取单品课程中的id列表
    	         * 倒序获取：因为缓存中是按创建时间递增排序，而需求是按照创建时间递减
    	         */
            	messageSet = jedis.zrevrangeByScore(messageSetKey, "+inf", "-inf", 0, pageCount);
            }
        }
		return messageSet;
	}

	/**
	 * 从缓存中读取saas课程留言详情，若缓存没有从数据库读取后写入缓存
	 * @param searchKeys String[]：第一个元素为留言所属的课程id，第二个元素为留言id
	 * @param readMessageReqEntity 缓存没有读取数据库时使用，function="findSaasCourseCommentByCommentId"
	 * @param operation
	 * @param cachedValue 从数据库读取后是否写进缓存
	 * @return
	 * @throws Exception
	 */
	public Map<String, String> readSaasCourseComment(String[] searchKeys, RequestEntity readMessageReqEntity,
			CommonReadOperation operation,Jedis jedis, boolean cachedValue) throws Exception {

		//拼接缓存key模式匹配的String[]
		String[] keyFields = {Constants.CACHED_KEY_COURSE_FIELD, Constants.CACHED_KEY_COMMENT_FIELD};
		
		Map<String,String> values = readData(searchKeys, Constants.CACHED_KEY_COURSE_SAAS_COMMENT_DETAIL, 
				keyFields, readMessageReqEntity, operation, jedis, cachedValue, -1);
		
		String commentId = values.get(Constants.CACHED_KEY_COMMENT_FIELD);	//缓存中的留言id
		if(!searchKeys[1].equals(commentId)){	//请求留言id的和缓存中的留言id一致
			Map<String, String> keyMap = new HashMap<String, String>();
			keyMap.put(Constants.CACHED_KEY_COURSE_FIELD, searchKeys[0]);	//course_id
			keyMap.put(Constants.CACHED_KEY_COMMENT_FIELD, searchKeys[1]);	//comment_id
			String key = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_SAAS_COMMENT_DETAIL, keyMap);
			jedis.del(key);
			values = readData(searchKeys, Constants.CACHED_KEY_COURSE_SAAS_COMMENT_DETAIL, 
					keyFields, readMessageReqEntity, operation, jedis, cachedValue, -1);
		}
		return values;
	}

	/**
	 * 读取系列课程的学员排序列表，包括zset的score（因为score=create_time）
	 * @param seriesId 系列id
	 * @param lastUserId 上一页最后一条数据的学员id
	 * @param pageCount 每页数量
	 * @param requestEntity 
	 * @param operation 
	 * @return
	 * @throws Exception 
	 */
	public Set<Tuple> readSeriesStudentSet(String seriesId, String lastUserId, int pageCount,
			RequestEntity requestEntity, CommonReadOperation operation,Jedis jedis) throws Exception {
		//返回结果集
		Set<Tuple> studentSet = null;
		
		Map<String, Object> keyMap = new HashMap<>();
        keyMap.put(Constants.CACHED_KEY_SERIES_FIELD, seriesId);
        String userSetKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_SERIES_USERS, keyMap);
        if(!jedis.exists(userSetKey)){
        	//缓存中不存在，从数据库中查询并写入缓存中
			List<Map<String, Object>> resultList = (List) operation.invokeProcess(requestEntity);
			if(resultList != null && !MiscUtils.isEmpty(resultList)){
				//生成zset的key
				String zsetKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_SERIES_USERS, 
						(Map<String, Object>)requestEntity.getParam());
				for(Map<String, Object> map : resultList){
					Long createTimeL = ((Date)map.get("create_time")).getTime();
					jedis.zadd(zsetKey, createTimeL, map.get("user_id").toString());
				}
			}
        }
        //获取上一页最后一条数据的score
        if(lastUserId != null && !"0".equals(lastUserId)){	//不是获取第一页数据
	        Double lastScore = jedis.zscore(userSetKey, lastUserId);
	        /*
	         * 分页获取学员的id列表，包括score（score=create_time）
	         * 倒序获取：因为缓存中是按创建时间递增排序，而需求是按照创建时间递减
	         */
	        studentSet = jedis.zrevrangeByScoreWithScores(userSetKey, "("+lastScore, "-inf", 0, pageCount);
        }else{	//获取第一页数据
	        /*
	         * 分页获取学员的id列表，包括score（score=create_time）
	         * 倒序获取：因为缓存中是按创建时间递增排序，而需求是按照创建时间递减
	         */
        	studentSet = jedis.zrevrangeByScoreWithScores(userSetKey, "+inf", "-inf", 0, pageCount);
        }
		return studentSet;
	}
	
	/**
	 * 分页读取用户加入的单品课程id列表
	 * @param userId 
	 * @param lastCourseId 上一页最后一条数据的课程id，null或0表示获取第一页
	 * @param pageCount
	 * @param requestEntity
	 * @param operation
	 * @return
	 * @throws Exception
	 */
	public Set<String> readUserCourseIdSet(String userId, String lastCourseId, int pageCount,
			RequestEntity requestEntity, CommonReadOperation operation,Jedis jedis) throws Exception {
		//返回结果集
		Set<String> courseIdSet = null;
		
		Map<String, Object> keyMap = new HashMap<>();
        keyMap.put(Constants.CACHED_KEY_USER_FIELD, userId);
        String userCoursesSetKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_USER_COURSES, keyMap);
        if(!jedis.exists(userCoursesSetKey)){
        	//缓存中不存在，从数据库中查询并写入缓存中
			List<Map<String, Object>> resultList = (List) operation.invokeProcess(requestEntity);
			if(resultList != null && !MiscUtils.isEmpty(resultList)){
				for(Map<String, Object> map : resultList){
					Long createTimeL = ((Date)map.get("create_time")).getTime();
					jedis.zadd(userCoursesSetKey, createTimeL, map.get("course_id").toString());
				}
			}
        }
        //获取上一页最后一条数据的score
        if(!MiscUtils.isEmpty(lastCourseId) && !"0".equals(lastCourseId)){	//不是获取第一页数据
	        Double lastScore = jedis.zscore(userCoursesSetKey, lastCourseId);
	        /*
	         * 分页获取课程的id列表
	         * 倒序获取：因为缓存中是按创建时间递增排序，而需求是按照创建时间递减
	         */
	        courseIdSet = jedis.zrevrangeByScore(userCoursesSetKey, "("+lastScore, "-inf", 0, pageCount);
        }else{	//获取第一页数据
	        /*
	         * 分页获取课程的id列表
	         * 倒序获取：因为缓存中是按创建时间递增排序，而需求是按照创建时间递减
	         */
        	courseIdSet = jedis.zrevrangeByScore(userCoursesSetKey, "+inf", "-inf", 0, pageCount);
        }
		return courseIdSet;
	}

	/**
	 * 分页读取用户在指定店铺加入的单品课程id列表
	 * @param userId
	 * @param shopId
	 * @param lastCourseId 上一页最后一条数据的课程id，null或0表示获取第一页
	 * @param pageCount
	 * @param requestEntity
	 * @param operation
	 * @return
	 * @throws Exception
	 */
	public Set<String> readUserShopCourseIdSet(String userId, String shopId, String lastCourseId, int pageCount,
			RequestEntity requestEntity, CommonReadOperation operation,Jedis jedis) throws Exception {
		//返回结果集
		Set<String> courseIdSet = null;
		
		Map<String, Object> keyMap = new HashMap<>();
        keyMap.put(Constants.CACHED_KEY_USER_FIELD, userId);
        keyMap.put(Constants.CACHED_KEY_SHOP_FIELD, shopId);
        String userShopCoursesSetKey = MiscUtils.getKeyOfCachedData(Constants.USER_SHOP_COURSE_ZSET, keyMap);
        if(!jedis.exists(userShopCoursesSetKey)){
        	//缓存中不存在，从数据库中查询并写入缓存中
			List<Map<String, Object>> resultList = (List) operation.invokeProcess(requestEntity);
			if(resultList != null && !MiscUtils.isEmpty(resultList)){
				for(Map<String, Object> map : resultList){
					Long createTimeL = ((Date)map.get("create_time")).getTime();
					jedis.zadd(userShopCoursesSetKey, createTimeL, map.get("course_id").toString());
				}
			}
        }
        //获取上一页最后一条数据的score
        if(!MiscUtils.isEmpty(lastCourseId) && !"0".equals(lastCourseId)){	//不是获取第一页数据
	        Double lastScore = jedis.zscore(userShopCoursesSetKey, lastCourseId);
	        /*
	         * 分页获取课程的id列表
	         * 倒序获取：因为缓存中是按创建时间递增排序，而需求是按照创建时间递减
	         */
	        courseIdSet = jedis.zrevrangeByScore(userShopCoursesSetKey, "("+lastScore, "-inf", 0, pageCount);
        }else{	//获取第一页数据
	        /*
	         * 分页获取课程的id列表
	         * 倒序获取：因为缓存中是按创建时间递增排序，而需求是按照创建时间递减
	         */
        	courseIdSet = jedis.zrevrangeByScore(userShopCoursesSetKey, "+inf", "-inf", 0, pageCount);
        }
		return courseIdSet;
	}
	
	/**
	 * 分页读取用户加入的系列课程id列表
	 * @param userId 
	 * @param lastSeriesId 上一页最后一条数据的系列id，null或0表示获取第一页
	 * @param pageCount
	 * @param requestEntity
	 * @param operation
	 * @param jedis
	 * @return
	 * @throws Exception
	 */
	public static Set<String> readUserSeriesIdSet(String userId, String lastSeriesId, int pageCount, 
			RequestEntity requestEntity, CommonReadOperation operation, Jedis jedis) throws Exception {
		//返回结果集
		Set<String> seriesIdSet = null;
		
		Map<String, Object> keyMap = new HashMap<>();
        keyMap.put(Constants.CACHED_KEY_USER_FIELD, userId);
        String userSeriesSetKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_USER_SERIES, keyMap);
        if(!jedis.exists(userSeriesSetKey)){
        	//缓存中不存在，从数据库中查询并写入缓存中
			List<Map<String, Object>> resultList = (List) operation.invokeProcess(requestEntity);
			if(resultList != null && !MiscUtils.isEmpty(resultList)){
				for(Map<String, Object> map : resultList){
					Long updateCourseTimeL = ((Date)map.get("update_course_time")).getTime();
					jedis.zadd(userSeriesSetKey, updateCourseTimeL, map.get("series_id").toString());
				}
			}
        }
        //获取上一页最后一条数据的score
        if(!MiscUtils.isEmpty(lastSeriesId) && !"0".equals(lastSeriesId)){	//不是获取第一页数据
	        Double lastScore = jedis.zscore(userSeriesSetKey, lastSeriesId);
	        /*
	         * 分页获取课程的id列表
	         * 倒序获取：因为缓存中是按创建时间递增排序，而需求是按照创建时间递减
	         */
	        seriesIdSet = jedis.zrevrangeByScore(userSeriesSetKey, "("+lastScore, "-inf", 0, pageCount);
        }else{	//获取第一页数据
	        /*
	         * 分页获取课程的id列表
	         * 倒序获取：因为缓存中是按创建时间递增排序，而需求是按照创建时间递减
	         */
        	seriesIdSet = jedis.zrevrangeByScore(userSeriesSetKey, "+inf", "-inf", 0, pageCount);
        }
		return seriesIdSet;
	}
	
	
	/**
	 * 分页读取用户在指定店铺加入的系列课程id列表
	 * @param userId
	 * @param shopId
	 * @param lastSeriesId 上一页最后一条数据的系列id，null或0表示获取第一页
	 * @param pageCount
	 * @param requestEntity
	 * @param operation
	 * @return
	 * @throws Exception
	 */
	public Set<String> readUserShopSeriesIdSet(String userId, String shopId, String lastSeriesId, int pageCount,
			RequestEntity requestEntity, CommonReadOperation operation,Jedis jedis) throws Exception {
		//返回结果集
		Set<String> seriesIdSet = null;
		
		Map<String, Object> keyMap = new HashMap<>();
        keyMap.put(Constants.CACHED_KEY_USER_FIELD, userId);
        keyMap.put(Constants.CACHED_KEY_SHOP_FIELD, shopId);
        String userShopSeriesSetKey = MiscUtils.getKeyOfCachedData(Constants.USER_SHOP_SERIES_ZSET, keyMap);
        if(!jedis.exists(userShopSeriesSetKey)){
        	//缓存中不存在，从数据库中查询并写入缓存中
			List<Map<String, Object>> resultList = (List) operation.invokeProcess(requestEntity);
			if(resultList != null && !MiscUtils.isEmpty(resultList)){
				for(Map<String, Object> map : resultList){
					Long updateCourseTimeL = ((Date)map.get("update_course_time")).getTime();
					jedis.zadd(userShopSeriesSetKey, updateCourseTimeL, map.get("course_id").toString());
				}
			}
        }
        //获取上一页最后一条数据的score
        if(!MiscUtils.isEmpty(lastSeriesId) && !"0".equals(lastSeriesId)){	//不是获取第一页数据
	        Double lastScore = jedis.zscore(userShopSeriesSetKey, lastSeriesId);
	        /*
	         * 分页获取课程的id列表
	         * 倒序获取：因为缓存中是按创建时间递增排序，而需求是按照创建时间递减
	         */
	        seriesIdSet = jedis.zrevrangeByScore(userShopSeriesSetKey, "("+lastScore, "-inf", 0, pageCount);
        }else{	//获取第一页数据
	        /*
	         * 分页获取课程的id列表
	         * 倒序获取：因为缓存中是按创建时间递增排序，而需求是按照创建时间递减
	         */
        	seriesIdSet = jedis.zrevrangeByScore(userShopSeriesSetKey, "+inf", "-inf", 0, pageCount);
        }
		return seriesIdSet;
	}
	
	/**
	 * 分页获取讲师所有的直播课程（单品和系列子课） Id集合
	 * @param lecturerId 讲师id
	 * @param startScore 上一页最后一条记录在该缓存zset中的排序score，获取首页传0
	 * @param pageCount
	 * @param requestEntity
	 * @param operation
	 * @param isAsc 是否在缓存中正序获取
	 * @return
	 * @throws Exception
	 */
	public Set<Tuple> readLecturerAllCourseIdSet(String lecturerId, long startScore, int pageCount,
			RequestEntity requestEntity, CommonReadOperation operation, boolean isAsc,Jedis jedis) throws Exception {
		//返回结果集
		Set<Tuple> courseSet = null;
		
		Map<String, Object> keyMap = new HashMap<>();
        keyMap.put(Constants.CACHED_KEY_SERVICE_LECTURER_FIELD, lecturerId);
        String lecturerCourseAllSetKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_ALL, keyMap);
        if(!jedis.exists(lecturerCourseAllSetKey)){
        	//缓存中不存在，从数据库中查询并写入缓存中
			List<Map<String, Object>> resultList = (List) operation.invokeProcess(requestEntity);
			if(resultList != null && !MiscUtils.isEmpty(resultList)){
				long scorse;
				for(Map<String, Object> map : resultList){
					scorse = MiscUtils.convertInfoToPostion(
							MiscUtils.convertObjectToLong(map.get("start_time")), 
							MiscUtils.convertObjectToLong(map.get("position")));
					jedis.zadd(lecturerCourseAllSetKey, scorse, map.get("course_id").toString());
				}
			}
        }
        
        if(0 != startScore){	//不是获取第一页数据
	        /*
	         * 分页获取课程的id列表
	         */
        	if (isAsc) {
        		courseSet = jedis.zrangeByScoreWithScores(lecturerCourseAllSetKey, "("+startScore, "+inf", 0, pageCount);
        	} else {
        		courseSet = jedis.zrevrangeByScoreWithScores(lecturerCourseAllSetKey, "("+startScore, "-inf", 0, pageCount);
        	}
        }else{	//获取第一页数据
	        /*
	         * 分页获取课程的id列表
	         */
        	if (isAsc) {
        		courseSet = jedis.zrangeByScoreWithScores(lecturerCourseAllSetKey, "-inf", "+inf", 0, pageCount);
        	} else {
        		courseSet = jedis.zrevrangeByScoreWithScores(lecturerCourseAllSetKey, "+inf", "-inf", 0, pageCount);
        	}
        }
        
		return courseSet;
	}

	/**
	 * 分页获取嘉宾所有预告和正在直播的直播课程（单品和系列子课） Id集合
	 * @param guestId
	 * @param startScore
	 * @param pageCount
	 * @param requestEntity
	 * @param operation
	 * @param isAsc 是否在缓存中正序获取
	 * @return
	 * @throws Exception 
	 */
	public Set<Tuple> readCuestCourseIdSet(String guestId, long startScore, int pageCount,
			RequestEntity requestEntity, CommonReadOperation operation, boolean isAsc,Jedis jedis) throws Exception {
		//返回结果集
		Set<Tuple> courseSet = null;
		
		Map<String, Object> keyMap = new HashMap<>();
        keyMap.put(Constants.GUEST_ID, guestId);
        String guestCourseAllSetKey = MiscUtils.getKeyOfCachedData(Constants.SYS_GUEST_COURSE_PREDICTION, keyMap);
        if(!jedis.exists(guestCourseAllSetKey)){
        	//缓存中不存在，从数据库中查询并写入缓存中
			List<Map<String, Object>> resultList = (List) operation.invokeProcess(requestEntity);
			if(resultList != null && !MiscUtils.isEmpty(resultList)){
				long scorse;
				for(Map<String, Object> map : resultList){
					scorse = MiscUtils.convertInfoToPostion(
							MiscUtils.convertObjectToLong(map.get("start_time")), 
							MiscUtils.convertObjectToLong(map.get("position")));
					jedis.zadd(guestCourseAllSetKey, scorse, map.get("course_id").toString());
				}
			}
        }
        
        if(0 != startScore){	//不是获取第一页数据
	        /*
	         * 分页获取课程的id列表
	         */
        	if (isAsc) {
        		courseSet = jedis.zrangeByScoreWithScores(guestCourseAllSetKey, "("+startScore, "+inf", 0, pageCount);
        	} else {
        		courseSet = jedis.zrevrangeByScoreWithScores(guestCourseAllSetKey, "("+startScore, "-inf", 0, pageCount);
        	}
        }else{	//获取第一页数据
	        /*
	         * 分页获取课程的id列表
	         */
        	if (isAsc) {
        		courseSet = jedis.zrangeByScoreWithScores(guestCourseAllSetKey, "-inf", "+inf", 0, pageCount);
        	} else {
        		courseSet = jedis.zrevrangeByScoreWithScores(guestCourseAllSetKey, "-inf", "+inf", 0, pageCount);
        	}
        }
        
		return courseSet;
	}
}

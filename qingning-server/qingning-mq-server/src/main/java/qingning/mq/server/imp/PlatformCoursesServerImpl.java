package qingning.mq.server.imp;

import com.alibaba.fastjson.JSON;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import qingning.common.entity.RequestEntity;
import qingning.common.util.Constants;
import qingning.common.util.JedisUtils;
import qingning.common.util.MiscUtils;
import qingning.db.common.mybatis.persistence.*;
import qingning.server.AbstractMsgService;
import qingning.server.JedisBatchCallback;
import qingning.server.JedisBatchOperation;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;

import java.util.*;

/**
 * Created by loovee on 2016/12/16.
 */
public class PlatformCoursesServerImpl extends AbstractMsgService {
	private static Logger log = LoggerFactory.getLogger(PlatformCoursesServerImpl.class);
	
    private CoursesMapper coursesMapper;
    private CourseAudioMapper courseAudioMapper;
    private CourseImageMapper courseImageMapper;

    @Override
    public void process(RequestEntity requestEntity, JedisUtils jedisUtils, ApplicationContext context) throws Exception {
        //将讲师的课程列表放入缓存中
        processPlatformCoursesCache(requestEntity, jedisUtils, context);
    }


    private void processPlatformCoursesCache(RequestEntity requestEntity, JedisUtils jedisUtils, ApplicationContext context) {
    	String appName = requestEntity.getAppName();
    	JedisBatchCallback callBack = (JedisBatchCallback)jedisUtils.getJedis(appName);
    	callBack.invoke(new JedisBatchOperation(){
    		@Override
    		public void batchOperation(Pipeline pipeline, Jedis jedis) {

    			String predictionListKey = Constants.CACHED_KEY_PLATFORM_COURSE_PREDICTION;
    			String finishListKey =  Constants.CACHED_KEY_PLATFORM_COURSE_FINISH;

    			String classifyPredictionListKey = Constants.CACHED_KEY_PLATFORM_COURSE_CLASSIFY_PREDICTION;
				String classifyFinishListKey =  Constants.CACHED_KEY_PLATFORM_COURSE_CLASSIFY_FINISH;
    			Map<String,String> classifyKeyMap = new HashMap<>();
    			jedis.del(predictionListKey);
    			jedis.del(finishListKey);
    			Map<String,Object> queryMap = new HashMap<>();
    			queryMap.put("status", "1");//已经发布(预告中)
    			long maxCount =  Constants.PLATFORM_PREDICTION_COURSE_LIST_SIZE;
    			queryMap.put("pageCount", maxCount);
    			queryMap.put("app_name",appName);
    			List<Map<String,Object>> coursePredictionList = coursesMapper.findPlatformCourseList(queryMap);
    			if(!MiscUtils.isEmpty(coursePredictionList)){
    				maxCount=maxCount-coursePredictionList.size();    				
    				for(Map<String,Object> values : coursePredictionList){
    					String course_id = (String)values.get("course_id");
    					try{
    						Date courseStartTime = (Date)values.get("start_time");
    						long position = MiscUtils.convertInfoToPostion(courseStartTime.getTime(), MiscUtils.convertObjectToLong(values.get("position")));
    						pipeline.zadd(predictionListKey, position,(String)values.get("course_id"));

							String classify_id = values.get("classify_id").toString();
							if(classifyKeyMap.containsKey(classify_id)){//存在
								pipeline.zadd(classifyKeyMap.get(classify_id), position,course_id);
							}else{//不存在
								Map<String,String> map = new HashMap<>();
								map.put(Constants.CACHED_KEY_CLASSIFY,classify_id);
								String classifyPredictionListKeyByClassify_id = MiscUtils.getKeyOfCachedData(classifyPredictionListKey, map);//分类
								jedis.del(classifyPredictionListKeyByClassify_id);
								classifyKeyMap.put(classify_id,classifyPredictionListKeyByClassify_id);
								pipeline.zadd(classifyPredictionListKeyByClassify_id, position,course_id);
							}
						} catch(Exception e){
    						log.error("course[id:"+course_id+"]"+e.getMessage());
        				}
    				}    				
    				pipeline.sync();
    			}
    			if(maxCount>0){
    				queryMap.put("status", "2");//已经结束
    				queryMap.put("pageCount", maxCount);
    				queryMap.put("app_name",appName);
    				List<Map<String,Object>> courseFinishList = coursesMapper.findPlatformCourseList(queryMap);
    				if(!MiscUtils.isEmpty(courseFinishList)){
    					Map<String,Response<Boolean>> checked = new HashMap<String,Response<Boolean>>();
    					for(Map<String,Object> values : courseFinishList){                			
    						String course_id =(String)values.get("course_id");
    						Map<String,Object> courseCacheMap = new HashMap<>();
							courseCacheMap.put(Constants.CACHED_KEY_COURSE_FIELD, course_id);
							String courseKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE, courseCacheMap);
							checked.put(course_id, pipeline.exists(courseKey));
    					}
    					pipeline.sync();
    					
    					for(Map<String,Object> values : courseFinishList){                			
    						String course_id =(String)values.get("course_id");
    						Date courseEndTime = (Date)values.get("end_time");
    						try{
    							long position = MiscUtils.convertInfoToPostion(courseEndTime.getTime(), MiscUtils.convertObjectToLong(values.get("position")));
    							pipeline.zadd(finishListKey, position,course_id);
    							Map<String,Object> courseCacheMap = new HashMap<>();
    							courseCacheMap.put(Constants.CACHED_KEY_COURSE_FIELD, course_id);
    							String courseKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE, courseCacheMap);
    							String pptsKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_PPTS, courseCacheMap);
    							String audiosKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_AUDIOS_JSON_STRING, courseCacheMap);
    							if(!checked.get(course_id).get()){
    								Map<String, String> strMap = new HashMap<String, String>();
    								MiscUtils.converObjectMapToStringMap(values, strMap);
    								pipeline.hmset(courseKey,strMap);
    								//PPT信息                                 
    								List<Map<String,Object>> pptList = courseImageMapper.findPPTListByCourseId(course_id);
    								if(! MiscUtils.isEmpty(pptList)){
    									pipeline.set(pptsKey, JSON.toJSONString(pptList));
    								}
    								//讲课音频信息                                
    								List<Map<String,Object>> audioList = courseAudioMapper.findAudioListByCourseId(course_id);
    								if(! MiscUtils.isEmpty(audioList)){
    									pipeline.set(audiosKey, JSON.toJSONString(audioList));
    								}
    							}

    							pipeline.expire(courseKey, Constants.CACHED_MAX_COURSE_TIME_LIFE);
    							pipeline.expire(pptsKey, Constants.CACHED_MAX_COURSE_TIME_LIFE);
    							pipeline.expire(audiosKey, Constants.CACHED_MAX_COURSE_TIME_LIFE);

								String classify_id = values.get("classify_id").toString();


								if(classifyKeyMap.containsKey(classify_id)){//存在
									pipeline.zadd(classifyKeyMap.get(classify_id), position,course_id);
								}else{//不存在
									Map<String,String> map = new HashMap<>();
									map.put(Constants.CACHED_KEY_CLASSIFY,classify_id);
									String classifyPredictionListKeyByClassify_id = MiscUtils.getKeyOfCachedData(classifyFinishListKey, map);//分类
									jedis.del(classifyPredictionListKeyByClassify_id);
									classifyKeyMap.put(classify_id,classifyPredictionListKeyByClassify_id);
									pipeline.zadd(classifyPredictionListKeyByClassify_id, position,course_id);
								}
    						} catch (Exception e){
    							log.warn("The course data["+course_id+"] is abnormal");
    						}
    					}
    					pipeline.sync();
    				}
    			}


				Set<String> classifyIdSet = jedis.zrange(Constants.CACHED_KEY_CLASSIFY_ALL, 0, -1);
				for(String classify_id : classifyIdSet){
					Map<String,Object> map = new HashMap<>();
					map.put("appName",appName);
					map.put("classify_id",classify_id);
					jedis.del(MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_PLATFORM_COURSE_CLASSIFY_FINISH, map));//分类
					jedis.del(MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_PLATFORM_COURSE_CLASSIFY_PREDICTION, map));//分类
				}

				Set<String> lecturerSet = jedis.smembers(Constants.CACHED_LECTURER_KEY);
				if(!MiscUtils.isEmpty(lecturerSet)){
					for(String lecturerId : lecturerSet) {
						//删除缓存中的旧的课程列表及课程信息实体
						Map<String, Object> map = new HashMap<>();
						map.put(Constants.CACHED_KEY_LECTURER_FIELD, lecturerId);
						predictionListKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_PREDICTION, map);
						finishListKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_FINISH, map);

						jedis.del(predictionListKey);
						jedis.del(finishListKey);

					}
				}
				for(String classify_id :classifyIdSet ){
					Map<String,Object> map = new HashMap<>();
					map.put("appName",appName);
					map.put("classify_id",classify_id);
					List<Map<String, Object>> courseByClassifyId = coursesMapper.findCourseByClassifyId(map);
					for(Map<String, Object> course : courseByClassifyId){
						if(course.get("status").equals("2") || course.get("status").equals("1")){
							String course_id = course.get("course_id").toString();
							String lecturer_id = course.get("lecturer_id").toString();
							map.put(Constants.CACHED_KEY_CLASSIFY, classify_id);//课程id
							map.put(Constants.CACHED_KEY_LECTURER_FIELD, lecturer_id);
							String courseClassifyIdKey = "";
							String courseLectureKey = "";
							Long time = 0L ;
							if(course.get("status").equals("2")){
								courseClassifyIdKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_PLATFORM_COURSE_CLASSIFY_FINISH, map);//分类
								courseLectureKey =  MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_FINISH, map);
								time = MiscUtils.convertObjectToLong(course.get("end_time"));//Long.valueOf(course.get("end_time").toString());

							}else{
								courseClassifyIdKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_PLATFORM_COURSE_CLASSIFY_PREDICTION, map);//分类
								courseLectureKey =  MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_PREDICTION, map);
								time = MiscUtils.convertObjectToLong(course.get("start_time"));//Long.valueOf(course.get("start_time").toString());

							}
							if(jedis.zrank(courseClassifyIdKey,course_id) ==  null ){
								map.put(Constants.CACHED_KEY_COURSE_FIELD, classify_id);//课程id
								String courseKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE, map);//"SYS:COURSE:{course_id}"
								long lpos = MiscUtils.convertInfoToPostion(time, MiscUtils.convertObjectToLong(jedis.hget(courseKey, "position")));
								jedis.zadd(courseClassifyIdKey, lpos,course_id);//在结束中增加
								jedis.zadd(courseLectureKey, lpos,course_id);//在结束中增加
							}
						}else if(course.get("status").equals("5")){
							String coursekey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE, course);//获取课程在缓存中的key
							jedis.hset(coursekey,"status","5");//把课程缓存中的状态改为已删除
						}
						map.clear();
					}
				}
				Map<String,Object> map = new HashMap<>();
				map.put("appName",appName);
				map.put("status","5");
				List<Map<String, Object>> courseIdList = coursesMapper.findCourseByStatus(map);
				for(Map<String, Object> courseid : courseIdList){
					String id = courseid.get("course_id").toString();
					jedis.zrem(Constants.CACHED_KEY_PLATFORM_COURSE_PREDICTION,id);
					jedis.zrem(Constants.CACHED_KEY_PLATFORM_COURSE_FINISH,id);
				}




    		}
    	});
    }

    public CoursesMapper getCoursesMapper() {
        return coursesMapper;
    }

    public void setCoursesMapper(CoursesMapper coursesMapper) {
        this.coursesMapper = coursesMapper;
    }

    public CourseAudioMapper getCourseAudioMapper() {
        return courseAudioMapper;
    }

    public void setCourseAudioMapper(CourseAudioMapper courseAudioMapper) {
        this.courseAudioMapper = courseAudioMapper;
    }

    public CourseImageMapper getCourseImageMapper() {
        return courseImageMapper;
    }

    public void setCourseImageMapper(CourseImageMapper courseImageMapper) {
        this.courseImageMapper = courseImageMapper;
    }
}

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
    	JedisBatchCallback callBack = (JedisBatchCallback)jedisUtils.getJedis();
    	callBack.invoke(new JedisBatchOperation(){
    		@Override
    		public void batchOperation(Pipeline pipeline, Jedis jedis) { 
    			String predictionListKey = Constants.CACHED_KEY_PLATFORM_COURSE_PREDICTION;
    			String finishListKey =  Constants.CACHED_KEY_PLATFORM_COURSE_FINISH;
    			jedis.del(predictionListKey);
    			jedis.del(finishListKey);
    			Map<String,Object> queryMap = new HashMap<>();
    			queryMap.put("status", "1");//已经发布(预告中)
    			long maxCount =  Constants.PLATFORM_PREDICTION_COURSE_LIST_SIZE;
    			queryMap.put("pageCount", maxCount);
    			List<Map<String,Object>> coursePredictionList = coursesMapper.findPlatformCourseList(queryMap);
    			if(!MiscUtils.isEmpty(coursePredictionList)){
    				maxCount=maxCount-coursePredictionList.size();
    				for(Map<String,Object> values : coursePredictionList){
    					Date courseStartTime = (Date)values.get("start_time");
    					pipeline.zadd(predictionListKey, courseStartTime.getTime(),(String)values.get("course_id"));
    				}
    				pipeline.sync();
    			}
    			if(maxCount>0){
    				queryMap.put("status", "2");//已经结束
    				queryMap.put("pageCount", maxCount);
    				List<Map<String,Object>> courseFinishList = coursesMapper.findPlatformCourseList(queryMap);
    				if(!MiscUtils.isEmpty(courseFinishList)){
    					for(Map<String,Object> values : courseFinishList){                			
    						String course_id =(String)values.get("course_id");
    						Date courseEndTime = (Date)values.get("end_time");
    						try{
    							pipeline.zadd(finishListKey, courseEndTime.getTime(),course_id);
    							Map<String,Object> courseCacheMap = new HashMap<>();
    							courseCacheMap.put(Constants.CACHED_KEY_COURSE_FIELD, course_id);
    							String courseKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE, courseCacheMap);
    							String pptsKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_PPTS, courseCacheMap);
    							String audiosKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_AUDIOS_JSON_STRING, courseCacheMap);
    							if(!jedis.exists(courseKey)){
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
    						} catch (Exception e){
    							log.warn("The course data["+course_id+"] is abnormal");
    						}
    					}
    					pipeline.sync();
    				}
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

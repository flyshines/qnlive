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
import redis.clients.jedis.Tuple;

import java.math.BigDecimal;
import java.util.*;

/**
 * Created by loovee on 2016/12/16.
 */
public class LecturerCoursesServerImpl extends AbstractMsgService {
	private static Logger log = LoggerFactory.getLogger(LecturerCoursesServerImpl.class);
    private CoursesMapper coursesMapper;
    private LoginInfoMapper loginInfoMapper;
    private CourseAudioMapper courseAudioMapper;
    private CourseImageMapper courseImageMapper;

    private MessagePushServerImpl messagePushServerimpl;

    @Override
    public void process(RequestEntity requestEntity, JedisUtils jedisUtils, ApplicationContext context) throws Exception {
        //将讲师的课程列表放入缓存中
        processLecturerCoursesCache(requestEntity, jedisUtils, context);
    }


    private void processLecturerCoursesCache(RequestEntity requestEntity, JedisUtils jedisUtils, ApplicationContext context) {
        Jedis jedis = jedisUtils.getJedis();

        ((JedisBatchCallback)jedisUtils.getJedis()).invoke(new JedisBatchOperation(){
        	private void processCached(Set<String> lecturerSet, Pipeline pipeline, Jedis jedis){
				for(String lecturerId : lecturerSet){
		            //删除缓存中的旧的课程列表及课程信息实体
		            Map<String,Object> map = new HashMap<>();
		            map.put(Constants.CACHED_KEY_LECTURER_FIELD, lecturerId);
		            String predictionListKey =  MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_PREDICTION, map);
		            String finishListKey =  MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_FINISH, map);
		            
		            Set<Tuple> predictionCourseIdList = jedis.zrangeWithScores(predictionListKey, 0 , -1);
		            Set<Tuple> finishCourseIdList = jedis.zrangeWithScores(finishListKey, 0 , -1);
		            jedis.del(predictionListKey);
		            jedis.del(finishListKey);
                    
                    Map<String,Object> queryMap = new HashMap<>();                   
                    double last_start_date = -1d;
                    double last_end_date = -1d;
                                       
                    int count = Constants.LECTURER_PREDICTION_COURSE_LIST_SIZE;
                    Map<String,Tuple> predictionCourseIdMap = new HashMap<String,Tuple>();
                    if(!MiscUtils.isEmpty(predictionCourseIdList)){                    	
                    	count=count-predictionCourseIdList.size();
                    	for(Tuple tuple : predictionCourseIdList){
                    		predictionCourseIdMap.put(tuple.getElement(), tuple);
                    		last_start_date = tuple.getScore();
                    	}
                    }
                    Map<String,Tuple> finishdictionCourseIdMap = new HashMap<String,Tuple>();
                    if(!MiscUtils.isEmpty(finishCourseIdList)){                    	
                    	for(Tuple tuple : finishCourseIdList){
                    		if(count <= 0){
                       		 	//删除课程实体、PPT实体、讲课音频实体
                        		queryMap.clear();
                        		queryMap.put(Constants.CACHED_KEY_COURSE_FIELD, tuple.getElement());
                                String courseKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE, queryMap);
                                String pptsKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_PPTS, queryMap);
                                String audiosKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_AUDIOS_JSON_STRING, queryMap);
                                pipeline.del(pptsKey);
                                pipeline.del(audiosKey);
                                pipeline.del(courseKey);
                    			continue;
                    		}
                    		--count;
                    		String key = tuple.getElement();
                    		predictionCourseIdMap.remove(key);
                    		finishdictionCourseIdMap.put(key, tuple);
                    		last_end_date = tuple.getScore();
                    	}
                    }
                    
                    if(!MiscUtils.isEmpty(predictionCourseIdMap)){
                    	for(String key:predictionCourseIdMap.keySet()){
                    		Tuple tuple = predictionCourseIdMap.get(key);
                    		pipeline.zadd(predictionListKey, tuple.getScore(), tuple.getElement());
                    	}
                    }
                    if(!MiscUtils.isEmpty(finishdictionCourseIdMap)){
                    	for(String key:finishdictionCourseIdMap.keySet()){
                    		Tuple tuple = finishdictionCourseIdMap.get(key);
                    		pipeline.zadd(finishListKey, tuple.getScore(), tuple.getElement());
                    	}
                    }
                    
                    if(last_start_date>0 || last_end_date>0){
                    	pipeline.sync();
                    }
                    
                    if(count>0){
                    	queryMap.clear();
                    	map.put(Constants.CACHED_KEY_LECTURER_FIELD, lecturerId);
                    	if(last_end_date>0){
                    		BigDecimal bigDecimal = new BigDecimal(last_end_date);
                    		bigDecimal.setScale(0, BigDecimal.ROUND_HALF_UP);                    		
                    		queryMap.put("end_time", new Date(bigDecimal.longValue()));
                    	} else if(last_start_date>0){
                    		BigDecimal bigDecimal = new BigDecimal(last_start_date);
                    		bigDecimal.setScale(0, BigDecimal.ROUND_HALF_UP);
                    		queryMap.put("start_time", new Date(bigDecimal.longValue()));
                    	}
                    	queryMap.put(Constants.CACHED_KEY_LECTURER_FIELD,lecturerId);
                    	queryMap.put("pageCount",count);
                    	List<Map<String,Object>> list = coursesMapper.findLecturerCourseList(queryMap);
                    	if(!MiscUtils.isEmpty(list)){
                    		for(Map<String,Object> value:list){
                    			String key = null;
                    			long score = -1;
                    			if("2".equals(value.get("status"))){                    				
                    				Date endDate = (Date)value.get("end_time");
                    				if(endDate != null){
                    					score = endDate.getTime();
                    					key = finishListKey;
                    				}                    				
                    			} else if("1".equals(value.get("status"))){
                    				Date startDate = (Date)value.get("start_time");
                    				if(startDate != null){
                    					score = startDate.getTime();
                    					key = predictionListKey;
                    				}   
                    			}
                    			if(!MiscUtils.isEmpty(key)){
                    				pipeline.zadd(key, score, (String)value.get("course_id"));
                    				
                    				map.clear();
                    				map.put(Constants.CACHED_KEY_COURSE_FIELD, (String)value.get("course_id"));
                    				String courseKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE, map);
                                    Map<String,String> courseStringMap = new HashMap<>();
                                    MiscUtils.converObjectMapToStringMap(value, courseStringMap);
                                    pipeline.hmset(courseKey,courseStringMap);                                   
                                    //PPT信息
                                    String pptsKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_PPTS, map);
                                    List<Map<String,Object>> pptList = courseImageMapper.findPPTListByCourseId((String)value.get("course_id"));
                                    if(! MiscUtils.isEmpty(pptList)){
                                        pipeline.set(pptsKey, JSON.toJSONString(pptList));
                                    }

                                    //讲课音频信息
                                    String audiosKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_AUDIOS_JSON_STRING, map);
                                    List<Map<String,Object>> audioList = courseAudioMapper.findAudioListByCourseId((String)value.get("course_id"));
                                    if(! MiscUtils.isEmpty(audioList)){
                                        pipeline.set(audiosKey, JSON.toJSONString(audioList));
                                    }                    				
                    			} else {
                    				log.warn("The course data ["+(String)value.get("course_id")+"] is abnormal");
                    			}
                    		}
                    		pipeline.sync();
                    	}
                    }
				}        		
        	}        	        	
			@Override
			public void batchOperation(Pipeline pipeline, Jedis jedis) {
				Set<String> lecturerSet = jedis.smembers(Constants.CACHED_LECTURER_KEY);
				if(!MiscUtils.isEmpty(lecturerSet)){
					processCached(lecturerSet, pipeline, jedis);					
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

    public LoginInfoMapper getLoginInfoMapper() {
        return loginInfoMapper;
    }

    public void setLoginInfoMapper(LoginInfoMapper loginInfoMapper) {
        this.loginInfoMapper = loginInfoMapper;
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


    public void setMessagePushServerimpl(MessagePushServerImpl messagePushServerimpl) {
        this.messagePushServerimpl = messagePushServerimpl;
    }
}

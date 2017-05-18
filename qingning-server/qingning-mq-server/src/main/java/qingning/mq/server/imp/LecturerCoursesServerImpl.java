package qingning.mq.server.imp;

import com.alibaba.fastjson.JSON;

import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import qingning.common.entity.RequestEntity;
import qingning.common.util.*;
import qingning.db.common.mybatis.persistence.*;
import qingning.server.AbstractMsgService;
import qingning.server.JedisBatchCallback;
import qingning.server.JedisBatchOperation;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;
import redis.clients.jedis.Tuple;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
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
    	String appName = requestEntity.getAppName();
        ((JedisBatchCallback)jedisUtils.getJedis(appName)).invoke(new JedisBatchOperation(){
        	private void processCached(Set<String> lecturerSet, Pipeline pipeline, Jedis jedis){
				for(String lecturerId : lecturerSet){
		            //删除缓存中的旧的课程列表及课程信息实体
		            Map<String,Object> map = new HashMap<>();
		            map.put(Constants.CACHED_KEY_LECTURER_FIELD, lecturerId);
		            String predictionListKey =  MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_PREDICTION, map);
		            String finishListKey =  MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_FINISH, map);

		            Set<Tuple> finishCourseIdList = jedis.zrangeWithScores(finishListKey, 0 , -1);
		            jedis.del(predictionListKey);
		            jedis.del(finishListKey);
                    
		            int count = 0;
		            int readCount = 0;
		            Date startTime = null;
		            
		            Map<String,Object> queryMap = new HashMap<>(); 
		            queryMap.put("status", "1");
		            queryMap.put("lecturer_id", lecturerId);
		            queryMap.put("pageCount", Constants.MAX_QUERY_LIMIT);
		            Map<String,Response<Boolean>> courseExists = new HashMap<String,Response<Boolean>>();
		            Map<String,Response<Boolean>> coursePptsExists = new HashMap<String,Response<Boolean>>();
		            Map<String,Response<Boolean>> courseAudioExists = new HashMap<String,Response<Boolean>>();
		            Map<String,Map<String,String>> courseValueMap = new HashMap<String,Map<String,String>>();
		            long now = System.currentTimeMillis();
		            do{		            
		            	if(startTime != null){
		            		queryMap.put("start_time", startTime);
		            	}
		            	List<Map<String,Object>> list = coursesMapper.findLecturerCourseListByStatus(queryMap);
		            	Map<String,Object> courseMap = new HashMap<String,Object>();
		            	if(!MiscUtils.isEmpty(list)){
		            		readCount=list.size();
		            		for(Map<String,Object> course : list){
		            			String course_id = (String)course.get("course_id");
		            			try{		            				
		            				startTime = (Date)course.get("start_time");
									long courseLiveOvertimeMsec = MiscUtils.convertObjectToLong(IMMsgUtil.configMap.get("course_live_overtime_msec"));//24小時毫秒值
									long taskStartTime = courseLiveOvertimeMsec + startTime.getTime();//当前课程加上24小时的毫秒值
									boolean result = (now - taskStartTime)>0;//判断课程是否超过24小时  如果大于就是不超过 如果是小于就是超过
									if(result){
										pipeline.zadd(predictionListKey, startTime.getTime(), course_id);
										courseMap.put(Constants.CACHED_KEY_COURSE_FIELD, course_id);

										String courseKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE, courseMap);
										String pptsKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_PPTS, courseMap);
										String audiosKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_AUDIOS_JSON_STRING, courseMap);

										courseExists.put(course_id, pipeline.exists(courseKey));
										coursePptsExists.put(course_id, pipeline.exists(pptsKey));
										courseAudioExists.put(course_id, pipeline.exists(audiosKey));

										Map<String,String> valueStrMap = new HashMap<String,String>();
										MiscUtils.converObjectMapToStringMap(course, valueStrMap);
										courseValueMap.put(course_id, valueStrMap);
									}else{
										messagePushServerimpl.processCourseEnd(coursesMapper,"2",course_id,jedis,appName);

									}


		            			} catch(Exception e){
		            				log.error("Load Course["+course_id+"]:"+e.getMessage());
		            			}
		            		}

		            		pipeline.sync();		            		
		            	} else {
		            		readCount=0;
		            	}
		            	count+=readCount;
		            } while(readCount == Constants.MAX_QUERY_LIMIT);
		            
		            
		            if(Constants.LECTURER_PREDICTION_COURSE_LIST_SIZE>count){
		            	int pageCount = Constants.LECTURER_PREDICTION_COURSE_LIST_SIZE-count;
		            	queryMap.put("status", "2");
		            	queryMap.remove("start_time");		            	
		            	queryMap.put("pageCount", pageCount);
		            	List<Map<String,Object>> list = coursesMapper.findLecturerCourseListByStatus(queryMap);		            	
		            	if(!MiscUtils.isEmpty(list)){
		            		Map<String,Object> courseMap = new HashMap<String,Object>();		            		
		            		for(Map<String,Object> course : list){
		            			String course_id = (String)course.get("course_id");
		            			try{		            				
		            				Date end_time = (Date)course.get("end_time");
		            				if(end_time==null){
		            					end_time = (Date)course.get("start_time");
		            				}
		            				pipeline.zadd(finishListKey, MiscUtils.convertInfoToPostion(end_time.getTime(), MiscUtils.convertObjectToLong(course.get("position"))), course_id);
		            				courseMap.put(Constants.CACHED_KEY_COURSE_FIELD, course_id);
		            				
		            				String courseKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE, courseMap);		        		            
		                            String pptsKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_PPTS, courseMap);
		                            String audiosKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_AUDIOS_JSON_STRING, courseMap);
		                            
		                            courseExists.put(course_id, pipeline.exists(courseKey));
		                            coursePptsExists.put(course_id, pipeline.exists(pptsKey));
		                            courseAudioExists.put(course_id, pipeline.exists(audiosKey));
		                            
		                            Map<String,String> valueStrMap = new HashMap<String,String>();
		                            MiscUtils.converObjectMapToStringMap(course, valueStrMap);
		                            courseValueMap.put(course_id, valueStrMap);
		            			} catch(Exception e){
		            				log.error("Load Course["+course_id+"]:"+e.getMessage());
		            			}
		            		}
		            		pipeline.sync();		            		
		            	}
		            }
		            
		            Map<String,Object> courseMap = new HashMap<String,Object>();
		            if(!MiscUtils.isEmpty(finishCourseIdList)){
			            for(Tuple tuple : finishCourseIdList){
			            	String course_id = tuple.getElement();
			            	if(!courseExists.containsKey(course_id)){
			            		courseMap.put(Constants.CACHED_KEY_COURSE_FIELD, course_id);     
	            				String courseKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE, courseMap);		        		            
	                            String pptsKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_PPTS, queryMap);
	                            String audiosKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_AUDIOS_JSON_STRING, queryMap);
	                            pipeline.del(pptsKey);
	                            pipeline.del(audiosKey);
	                            pipeline.del(courseKey);
			            	}
			            }
		            }
		            pipeline.sync();
		            for(String courseId:courseExists.keySet()){
		            	courseMap.put(Constants.CACHED_KEY_COURSE_FIELD, courseId);        				
        				String courseKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE, courseMap);		        		            
                        String pptsKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_PPTS, courseMap);
                        String audiosKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_AUDIOS_JSON_STRING, courseMap);
		            	if(!courseExists.get(courseId).get()){
		            		pipeline.hmset(courseKey, courseValueMap.get(courseId));
		            	}
		            	if(!coursePptsExists.get(courseId).get()){
                            List<Map<String,Object>> pptList = courseImageMapper.findPPTListByCourseId(courseId);
                            if(! MiscUtils.isEmpty(pptList)){
                                pipeline.set(pptsKey, JSON.toJSONString(pptList));
                            }
		            	}
		            	if(!courseAudioExists.get(courseId).get()){
			            	List<Map<String,Object>> audioList = courseAudioMapper.findAudioListByCourseId(courseId);
	                        if(! MiscUtils.isEmpty(audioList)){
	                            pipeline.set(audiosKey, JSON.toJSONString(audioList));
	                        }
		            	}     
		            }
		            pipeline.sync();
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

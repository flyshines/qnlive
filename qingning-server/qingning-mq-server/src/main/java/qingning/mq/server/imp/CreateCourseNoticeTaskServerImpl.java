package qingning.mq.server.imp;

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
import redis.clients.jedis.Tuple;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


public class CreateCourseNoticeTaskServerImpl extends AbstractMsgService {

	private CoursesMapper coursesMapper;
	private MessagePushServerImpl messagePushServerImpl;
	@Override
	public void process(RequestEntity requestEntity, final JedisUtils jedisUtils, ApplicationContext context) throws Exception {
		if(messagePushServerImpl==null){
			messagePushServerImpl = (MessagePushServerImpl)context.getBean("MessagePushServer");
		}
		((JedisBatchCallback)jedisUtils.getJedis()).invoke(new JedisBatchOperation(){
			@Override
			public void batchOperation(Pipeline pipeline, Jedis jedis) {
				Set<String> lecturerSet = jedis.smembers(Constants.CACHED_LECTURER_KEY);
				processCreateCourseNoticeTask(lecturerSet, pipeline, jedis, jedisUtils, context);
			}    		 
		});
	}

    private void processCreateCourseNoticeTask(Set<String> lecturerSet, Pipeline pipeline, 
    		Jedis jedis, JedisUtils jedisUtils, ApplicationContext context){
    	for(String lecturerId : lecturerSet){
    		Map<String,Object> map = new HashMap<>();
            map.put(Constants.CACHED_KEY_LECTURER_FIELD, lecturerId);
            String predictionListKey =  MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_PREDICTION, map);
            Set<Tuple> predictionCourseIdList = jedis.zrangeWithScores(predictionListKey, 0 , -1);
            if(MiscUtils.isEmpty(predictionCourseIdList)){
            	continue;
            }
            Map<String, Response<String>> timeMap = new HashMap<>();
            Map<String, Response<String>> titleMap = new HashMap<>();
            for(Tuple tuple : predictionCourseIdList){
            	String courseId = tuple.getElement();
				map.clear();
				map.put(Constants.CACHED_KEY_COURSE_FIELD, courseId);
				String courseKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE, map);
				timeMap.put(courseId, pipeline.hget(courseKey, "start_time"));
				titleMap.put(courseId, pipeline.hget(courseKey, "course_title"));
            }
            pipeline.sync();
            long currentTime = System.currentTimeMillis();
            long currentDate = MiscUtils.getDate(currentTime);
            for(String courseId : timeMap.keySet()){
            	long time = Long.parseLong(timeMap.get(courseId).get());
            	long date = MiscUtils.getDate(currentDate);
            	String course_title =MiscUtils.convertString(titleMap.get(courseId).get());
            	map.clear();
            	map.put("course_id", courseId);
            	map.put("start_time", new Date(time));
            	map.put("lecturer_id", lecturerId);
            	map.put("course_title", course_title);
            	if(currentDate==date && time>currentTime){
                    RequestEntity requestEntity = generateRequestEntity("MessagePushServer", Constants.MQ_METHOD_ASYNCHRONIZED, "processCourseNotStart", map);
                    messagePushServerImpl.processForceEndCourse(requestEntity, jedisUtils, context);
                    
                    RequestEntity requestEntityTask = generateRequestEntity("MessagePushServer", Constants.MQ_METHOD_ASYNCHRONIZED,"processCourseStartShortNotice",map);
                    //提前五分钟开课提醒
                    messagePushServerImpl.processCourseStartShortNotice(requestEntityTask, jedisUtils, context);

                    //开课时间到但是讲师未出现提醒
                    requestEntityTask.setFunctionName("processCourseStartLecturerNotShow");
                    messagePushServerImpl.processCourseStartLecturerNotShow(requestEntityTask, jedisUtils, context);

                    //提醒学生参加课程定时任务
                    //requestEntityTask.setFunctionName("processCourseStartStudentStudyNotice");//TODO
                    //messagePushServerImpl.processCourseStartStudentStudyNotice(requestEntityTask, jedisUtils, context);
                    
            	} else if((date-currentDate)/(1000*60*24) == 1){
                    RequestEntity requestEntityTask =  generateRequestEntity("MessagePushServer", Constants.MQ_METHOD_ASYNCHRONIZED, "processCourseStartLongNotice", map);                   
                    messagePushServerImpl.processCourseStartLongNotice(requestEntityTask, jedisUtils, context);
            	}
            	
            	
            }
    	}
    }

    public CoursesMapper getCoursesMapper() {
        return coursesMapper;
    }

    public void setCoursesMapper(CoursesMapper coursesMapper) {
        this.coursesMapper = coursesMapper;
    }
}

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
		String appName = requestEntity.getAppName();
		((JedisBatchCallback)jedisUtils.getJedis(appName)).invoke(new JedisBatchOperation(){
			@Override
			public void batchOperation(Pipeline pipeline, Jedis jedis) {
				Set<String> lecturerSet = jedis.smembers(Constants.CACHED_LECTURER_KEY);
				processCreateCourseNoticeTask(lecturerSet, pipeline, jedis, jedisUtils, context,appName);
			}    		 
		});
	}

    private void processCreateCourseNoticeTask(Set<String> lecturerSet, Pipeline pipeline, 
    		Jedis jedis, JedisUtils jedisUtils, ApplicationContext context,String appName){
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
            Map<String, Response<String>> positionMap = new HashMap<>();
            Map<String, Response<String>> IMCourseIdMap = new HashMap<>();
			Map<String, Response<String>> realStartTimeMap = new HashMap<String, Response<String>>();
            for(Tuple tuple : predictionCourseIdList){
            	String courseId = tuple.getElement();
				map.clear();
				map.put(Constants.CACHED_KEY_COURSE_FIELD, courseId);
				String courseKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE, map);
				timeMap.put(courseId, pipeline.hget(courseKey, "start_time"));
				titleMap.put(courseId, pipeline.hget(courseKey, "course_title"));
				positionMap.put(courseId, pipeline.hget(courseKey, "position"));
				IMCourseIdMap.put(courseId, pipeline.hget(courseKey, "im_course_id"));
				realStartTimeMap.put(courseId, pipeline.hget(courseKey, "real_start_time"));
            }
            pipeline.sync();
            long currentTime = System.currentTimeMillis();
            long currentDate = MiscUtils.getDate(currentTime);
            for(String courseId : timeMap.keySet()){
            	long time = Long.parseLong(timeMap.get(courseId).get());
            	long date = MiscUtils.getDate(time);
            	String course_title =MiscUtils.convertString(titleMap.get(courseId).get());
            	long position = MiscUtils.convertObjectToLong(positionMap.get(courseId).get());
				String im_course_id = IMCourseIdMap.get(courseId).get();
				long realStartTime = MiscUtils.convertObjectToLong(realStartTimeMap.get(courseId).get());
            	map.clear();
            	map.put("course_id", courseId);
            	map.put("start_time", new Date(time));
            	map.put("lecturer_id", lecturerId);
            	map.put("course_title", course_title);
            	map.put("position", position);
            	map.put("im_course_id", im_course_id);
            	if(time>0){
            		map.put("real_start_time", time);
            	}
            	boolean isTheSameDate = MiscUtils.isTheSameDate(new Date(time), new Date());
            	if(time<=currentTime || isTheSameDate){//开课时间小于当前时间或者是今天
                    RequestEntity requestEntity = generateRequestEntity("MessagePushServer", Constants.MQ_METHOD_ASYNCHRONIZED, "processCourseNotStartCancelAll", map);
					requestEntity.setAppName(appName);
                    if(time>0){

						//清除所有定时任务
                    	messagePushServerImpl.processCourseNotStartCancelAll(requestEntity, jedisUtils, context);

						//提前60分钟开课提醒
						requestEntity.setFunctionName("processCourseStartShortNotice");
						messagePushServerImpl.processCourseStartShortNotice(requestEntity, jedisUtils, context);

						//课程开课提醒
						requestEntity.setFunctionName("processCourseStartIM");
						messagePushServerImpl.processCourseStartIM(requestEntity,jedisUtils,context);

						//讲师未出现提醒
						requestEntity.setFunctionName("processCourseStartLecturerNotShow");
						messagePushServerImpl.processCourseStartLecturerNotShow(requestEntity, jedisUtils, context);

						//课程超时提醒
						requestEntity.setFunctionName("processLiveCourseOvertimeNotice");
						messagePushServerImpl.processLiveCourseOvertimeNotice(requestEntity, jedisUtils, context);

						//课程超时强制结束
						requestEntity.setFunctionName("processCourseLiveOvertime");
                    	messagePushServerImpl.processCourseLiveOvertime(requestEntity, jedisUtils, context);
                    }
            	}
            	if(time>currentTime){//开课时间大于当前时间
	            	if(isTheSameDate){
	                    RequestEntity requestEntityTask = generateRequestEntity("MessagePushServer", Constants.MQ_METHOD_ASYNCHRONIZED,"processCourseStartShortNotice",map);
						requestEntityTask.setAppName(appName);

						//清除所有定时任务
						messagePushServerImpl.processCourseNotStartCancelAll(requestEntityTask, jedisUtils, context);

	                     //提前60分钟开课提醒
						requestEntityTask.setFunctionName("processCourseStartShortNotice");
	                    messagePushServerImpl.processCourseStartShortNotice(requestEntityTask, jedisUtils, context);

						//课程开课提醒
						requestEntityTask.setFunctionName("processCourseStartIM");
						messagePushServerImpl.processCourseStartIM(requestEntityTask,jedisUtils,context);

	                    //讲师未出现提醒
	                    requestEntityTask.setFunctionName("processCourseStartLecturerNotShow");
	                    messagePushServerImpl.processCourseStartLecturerNotShow(requestEntityTask, jedisUtils, context);


						//课程超时提醒
						requestEntityTask.setFunctionName("processLiveCourseOvertimeNotice");
						messagePushServerImpl.processLiveCourseOvertimeNotice(requestEntityTask, jedisUtils, context);


						//课程超时强制结束
						requestEntityTask.setFunctionName("processCourseLiveOvertime");
						messagePushServerImpl.processCourseLiveOvertime(requestEntityTask, jedisUtils, context);

	            	} else if((date-currentDate)/(1000*60*60*24) == 1){
	            		//24小时提醒
	                    RequestEntity requestEntityTask =  generateRequestEntity("MessagePushServer", Constants.MQ_METHOD_ASYNCHRONIZED, "processCourseStartLongNotice", map);                   
	                    messagePushServerImpl.processCourseStartLongNotice(requestEntityTask, jedisUtils, context);
	            	}
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

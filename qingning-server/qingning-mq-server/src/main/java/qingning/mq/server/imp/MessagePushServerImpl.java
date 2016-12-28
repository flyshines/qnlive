package qingning.mq.server.imp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import qingning.common.entity.QNLiveException;
import qingning.common.entity.RequestEntity;
import qingning.common.util.Constants;
import qingning.common.util.IMMsgUtil;
import qingning.common.util.JedisUtils;
import qingning.common.util.MiscUtils;
import qingning.mq.persistence.entity.Courses;
import qingning.mq.persistence.entity.LiveRoom;
import qingning.mq.persistence.mybatis.*;
import qingning.server.AbstractMsgService;
import qingning.server.annotation.FunctionName;
import qingning.server.rabbitmq.MessageServer;
import redis.clients.jedis.Jedis;

import java.util.*;

/**
 * Created by loovee on 2016/12/20.
 */
public class MessagePushServerImpl extends AbstractMsgService {

    private static Logger log = LoggerFactory.getLogger(ImMsgServiceImp.class);

    private Map<String, TimerTask> processCourseNotStartMap = new Hashtable<>();
    @Autowired
    private CoursesMapper coursesMapper;

    private SaveCourseMessageService saveCourseMessageService;
    private SaveCourseAudioService saveCourseAudioService;

    static Timer timer;
    static {
        timer = new Timer();
    }


    //课程未开播处理定时任务
    @FunctionName("processCourseNotStart")
    public void processCourseNotStart(RequestEntity requestEntity, JedisUtils jedisUtils, ApplicationContext context) {
        Jedis jedis = jedisUtils.getJedis();
        Map<String, Object> reqMap = (Map<String, Object>) requestEntity.getParam();
        log.debug("---------------将课程加入未开播处理定时任务"+reqMap);
        String courseId = reqMap.get("course_id").toString();
        long startTime = Long.parseLong(reqMap.get("start_time").toString());
        saveCourseMessageService = (SaveCourseMessageService)context.getBean("SaveCourseMessageServer");
        saveCourseAudioService = (SaveCourseAudioService)context.getBean("SaveAudioMessageServer");
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                log.debug("-----------课程未开播处理定时任务 课程id"+courseId+"  执行时间"+System.currentTimeMillis());
                processCourseEnd(coursesMapper, "1", jedisUtils, courseId, jedis, saveCourseMessageService, saveCourseAudioService);
                TimerTask timerTask = processCourseNotStartMap.remove(courseId);
                timerTask.cancel();
                timer.purge();
            }
        };

        processCourseNotStartMap.put(courseId, timerTask);
        String processCourseNotStartTime = IMMsgUtil.configMap.get("course_not_start_time_msec");
        long taskTime = Long.parseLong(processCourseNotStartTime) + startTime;
        timer.schedule(timerTask, new Date(taskTime));
    }

    //课程未开播处理定时任务取消
    @FunctionName("processCourseNotStartCancel")
    public void processCourseNotStartCancel(RequestEntity requestEntity, JedisUtils jedisUtils, ApplicationContext context) {
        Map<String, Object> reqMap = (Map<String, Object>) requestEntity.getParam();
        String courseId = reqMap.get("course_id").toString();
        TimerTask timerTask = processCourseNotStartMap.remove(courseId);
        if(timerTask != null){
            timerTask.cancel();
            timer.purge();
        }
    }

    //课程未开播处理定时任务取消
    @FunctionName("processCourseNotStartUpdate")
    public void processCourseNotStartUpdate(RequestEntity requestEntity, JedisUtils jedisUtils, ApplicationContext context) {
        processCourseNotStartCancel(requestEntity,jedisUtils,context);
        processCourseNotStart(requestEntity,jedisUtils,context);
    }

    //课程直播超时处理
    @FunctionName("processCourseLiveOvertime")
    public void processCourseLiveOvertime(RequestEntity requestEntity, JedisUtils jedisUtils, ApplicationContext context){
        Jedis jedis = jedisUtils.getJedis();
        Map<String, Object> reqMap = (Map<String, Object>) requestEntity.getParam();
        log.debug("---------------将课程加入直播超时处理定时任务"+reqMap);
        String courseId = reqMap.get("course_id").toString();
        long realStartTime = Long.parseLong(reqMap.get("real_start_time").toString());
        saveCourseMessageService = (SaveCourseMessageService)context.getBean("SaveCourseMessageServer");
        saveCourseAudioService = (SaveCourseAudioService)context.getBean("SaveAudioMessageServer");
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                log.debug("课程直播超时处理定时任务 课程id"+courseId+"  执行时间"+System.currentTimeMillis());
                processCourseEnd(coursesMapper,"2",jedisUtils,courseId,jedis,saveCourseMessageService,saveCourseAudioService);
                TimerTask timerTask = processCourseNotStartMap.remove(courseId);
                timerTask.cancel();
                timer.purge();
            }
        };

        processCourseNotStartMap.put(courseId, timerTask);
        String courseLiveOvertimeMsec = IMMsgUtil.configMap.get("course_live_overtime_msec");
        long taskTime = Long.parseLong(courseLiveOvertimeMsec) + realStartTime;
        timer.schedule(timerTask, new Date(taskTime));
    }

    //type 为1则为课程未开播强制结束，type为2则为课程直播超时强制结束
    private void processCourseEnd(CoursesMapper processCoursesMapper, String type ,JedisUtils jedisUtils, String courseId, Jedis jedis, SaveCourseMessageService saveCourseMessageService, SaveCourseAudioService saveCourseAudioService){
        Map<String,Object> map = new HashMap<>();
        map.put(Constants.CACHED_KEY_COURSE_FIELD, courseId);
        String courseKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE, map);
        Map<String,String> courseMap = jedis.hgetAll(courseKey);
        String realStartTime = courseMap.get("real_start_time");

        boolean processFlag = false;
        if(type.equals("1")){
            if(MiscUtils.isEmpty(realStartTime)){
                processFlag = true;
            }
        }else if(type.equals("2")){
            if(! courseMap.get("status").equals("2")){
                processFlag = true;
            }
        }

        //如果为未开播课程，则对课程进行结束处理
        if(processFlag){
            //1.1如果为课程结束，则取当前时间为课程结束时间
            Date now = new Date();
            //1.2更新课程详细信息（变更课程为已经结束）
            Courses courses = new Courses();
            courses.setCourseId(courseId);
            courses.setEndTime(now);
            courses.setUpdateTime(now);
            courses.setStatus("2");
            processCoursesMapper.updateByPrimaryKeySelective(courses);

            //1.3将该课程从讲师的预告课程列表 SYS: lecturer:{ lecturer_id }：courses  ：prediction移动到结束课程列表 SYS: lecturer:{ lecturer_id }：courses  ：finish
            map.clear();
            map.put(Constants.CACHED_KEY_LECTURER_FIELD, courseMap.get("lecturer_id"));
            String lecturerCoursesPredictionKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_PREDICTION, map);
            jedis.zrem(lecturerCoursesPredictionKey, courseId);

            String lecturerCoursesFinishKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_FINISH, map);
            String courseStartTime = jedis.hget(courseKey, "start_time");
            jedis.zadd(lecturerCoursesFinishKey, (double) now.getTime(), courseId);

            //1.4将该课程从平台的预告课程列表 SYS：courses  ：prediction移除。如果存在结束课程列表 SYS：courses ：finish，则增加到课程结束列表
            jedis.zrem(Constants.CACHED_KEY_PLATFORM_COURSE_PREDICTION, courseId);
            if(jedis.exists(Constants.CACHED_KEY_PLATFORM_COURSE_FINISH)){
                jedis.zadd(Constants.CACHED_KEY_PLATFORM_COURSE_FINISH, (double) now.getTime(), courseId);
            }

            //1.5如果课程标记为结束，则清除该课程的禁言缓存数据
            map.put(Constants.CACHED_KEY_COURSE_FIELD, courseMap.get("lecturer_id"));
            String banKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_BAN_USER_LIST, map);
            jedis.del(banKey);

            //1.6更新课程缓存信息
            Map<String, String> updateCacheMap = new HashMap<String, String>();
            updateCacheMap.put("update_time", courses.getEndTime().getTime() + "");
            updateCacheMap.put("end_time", courses.getEndTime().getTime() + "");
            updateCacheMap.put("status", "2");
            jedis.hmset(courseKey, updateCacheMap);

            //1.7如果存在课程聊天信息
            RequestEntity messageRequestEntity = new RequestEntity();
            Map<String,Object> processMap = new HashMap<>();
            processMap.put("course_id", courseId);
            messageRequestEntity.setParam(processMap);
            try {
                saveCourseMessageService.process(messageRequestEntity, jedisUtils, null);
            } catch (Exception e) {
                //TODO 暂时不处理
            }

            //1.8如果存在课程音频信息
            RequestEntity audioRequestEntity = new RequestEntity();
            audioRequestEntity.setParam(processMap);
            try {
                saveCourseAudioService.process(audioRequestEntity, jedisUtils, null);
            } catch (Exception e) {
                //TODO 暂时不处理
            }
        }
    }

    public CoursesMapper getCoursesMapper() {
        return coursesMapper;
    }

    public void setCoursesMapper(CoursesMapper coursesMapper) {
        this.coursesMapper = coursesMapper;
    }

    public SaveCourseMessageService getSaveCourseMessageService() {
        return saveCourseMessageService;
    }

    public void setSaveCourseMessageService(SaveCourseMessageService saveCourseMessageService) {
        this.saveCourseMessageService = saveCourseMessageService;
    }

    public SaveCourseAudioService getSaveCourseAudioService() {
        return saveCourseAudioService;
    }

    public void setSaveCourseAudioService(SaveCourseAudioService saveCourseAudioService) {
        this.saveCourseAudioService = saveCourseAudioService;
    }
}

package qingning.mq.server.imp;

import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import qingning.common.entity.RequestEntity;
import qingning.common.util.*;
import qingning.mq.persistence.entity.Courses;
import qingning.mq.persistence.mybatis.CoursesMapper;
import qingning.mq.persistence.mybatis.CoursesStudentsMapper;
import qingning.server.AbstractMsgService;
import qingning.server.annotation.FunctionName;
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

    @Autowired
    private CoursesStudentsMapper coursesStudentsMapper;


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

    @FunctionName("processLiveCourseOvertimeNotice")
    public void processLiveCourseOvertimeNotice(RequestEntity requestEntity, JedisUtils jedisUtils, ApplicationContext context) {
        Jedis jedis = jedisUtils.getJedis();
        Map<String, Object> reqMap = (Map<String, Object>) requestEntity.getParam();
        log.debug("---------------将课程加入直播超时预先提醒定时任务"+reqMap);
        String courseId = reqMap.get("course_id").toString();
        long real_start_time = Long.parseLong(reqMap.get("real_start_time").toString());
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                Map<String,Object> map = new HashMap<>();
                map.put(Constants.CACHED_KEY_COURSE_FIELD, courseId);
                String courseKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE, map);
                Map<String,String> courseMap = jedis.hgetAll(courseKey);
                if(MiscUtils.isEmpty(courseMap)  || courseMap.get("status").equals("2")){
                    TimerTask timerTask = processCourseNotStartMap.remove(courseId);
                    timerTask.cancel();
                    timer.purge();
                    return;
                }
                log.debug("-----------课程加入直播超时预先提醒定时任务 课程id"+courseId+"  执行时间"+System.currentTimeMillis());
                JSONObject obj = new JSONObject();
                obj.put("body",MiscUtils.getConfigByKey("jpush_course_live_overtime_per_notice"));
                obj.put("to",courseMap.get("lecturer_id"));
                obj.put("msg_type","4");
                Map<String,String> extrasMap = new HashMap<>();
                extrasMap.put("msg_type","4");
                extrasMap.put("course_id",courseId);
                obj.put("extras_map", extrasMap);
                JPushHelper.push(obj);
                TimerTask timerTask = processCourseNotStartMap.remove(courseId);
                timerTask.cancel();
                timer.purge();
            }
        };

        processCourseNotStartMap.put(courseId, timerTask);
        String courseOvertime = MiscUtils.getConfigByKey("course_live_overtime_msec");
        long taskTime = Long.parseLong(courseOvertime) + real_start_time - 10*60*1000;
        timer.schedule(timerTask, new Date(taskTime));
    }

    @FunctionName("processCourseStartLongNotice")
    public void processCourseStartLongNotice(RequestEntity requestEntity, JedisUtils jedisUtils, ApplicationContext context) {

        Map<String, Object> reqMap = (Map<String, Object>) requestEntity.getParam();
        log.debug("---------------将课程加入开播预先24H提醒定时任务"+reqMap);
        String courseId = reqMap.get("course_id").toString();
        String lecturer_id = reqMap.get("lecturer_id").toString();
        String course_title = reqMap.get("course_title").toString();
        long start_time = ((Date)(reqMap.get("start_time"))).getTime();
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {

                log.debug("-----------开播预先24H提醒定时任务 课程id"+courseId+"  执行时间"+System.currentTimeMillis());
                String fotmatString = "HH:mm";
                String startTimeFormat = MiscUtils.parseDateToFotmatString((Date)(reqMap.get("start_time")),fotmatString);
                JSONObject obj = new JSONObject();
                obj.put("body",String.format(MiscUtils.getConfigByKey("jpush_course_start_per_long_notice"), course_title,startTimeFormat));
                obj.put("to",lecturer_id);
                obj.put("msg_type","1");
                Map<String,String> extrasMap = new HashMap<>();
                extrasMap.put("msg_type","1");
                extrasMap.put("course_id",courseId);
                obj.put("extras_map", extrasMap);
                JPushHelper.push(obj);
                TimerTask timerTask = processCourseNotStartMap.remove(courseId);
                timerTask.cancel();
                timer.purge();
            }
        };

        processCourseNotStartMap.put(courseId, timerTask);
        long noticeTime= 24*60*60*1000;
        long taskTime = start_time - noticeTime;
        timer.schedule(timerTask, new Date(taskTime));
    }


    @FunctionName("processCourseStartShortNotice")
    public void processCourseStartShortNotice(RequestEntity requestEntity, JedisUtils jedisUtils, ApplicationContext context) {

        Map<String, Object> reqMap = (Map<String, Object>) requestEntity.getParam();
        log.debug("---------------将课程加入开播预先5min提醒定时任务"+reqMap);
        String courseId = reqMap.get("course_id").toString();
        String lecturer_id = reqMap.get("lecturer_id").toString();
        String course_title = reqMap.get("course_title").toString();
        long start_time = ((Date)(reqMap.get("start_time"))).getTime();
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {

                log.debug("-----------开播预先5min提醒定时任务 课程id"+courseId+"  执行时间"+System.currentTimeMillis());
                JSONObject obj = new JSONObject();
                obj.put("body",String.format(MiscUtils.getConfigByKey("jpush_course_start_per_short_notice"), course_title,"5"));
                obj.put("to",lecturer_id);
                obj.put("msg_type","2");
                Map<String,String> extrasMap = new HashMap<>();
                extrasMap.put("msg_type","2");
                extrasMap.put("course_id",courseId);
                obj.put("extras_map", extrasMap);
                JPushHelper.push(obj);
                TimerTask timerTask = processCourseNotStartMap.remove(courseId);
                timerTask.cancel();
                timer.purge();
            }
        };

        processCourseNotStartMap.put(courseId, timerTask);
        long noticeTime= 5*60*1000;
        long taskTime = start_time - noticeTime;
        timer.schedule(timerTask, new Date(taskTime));
    }

    @FunctionName("processCourseStartStudentStudyNotice")
    public void processCourseStartStudentStudyNotice(RequestEntity requestEntity, JedisUtils jedisUtils, ApplicationContext context) {

        Map<String, Object> reqMap = (Map<String, Object>) requestEntity.getParam();
        log.debug("---------------加入学生上课3min提醒定时任务"+reqMap);
        String courseId = reqMap.get("course_id").toString();
        String lecturer_id = reqMap.get("lecturer_id").toString();
        String course_title = reqMap.get("course_title").toString();
        long start_time = ((Date)(reqMap.get("start_time"))).getTime();
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {

                log.debug("-----------加入学生上课3min提醒定时任务 课程id"+courseId+"  执行时间"+System.currentTimeMillis());
                JSONObject obj = new JSONObject();
                obj.put("body",String.format(MiscUtils.getConfigByKey("jpush_course_study_notice"), course_title));
                List<String> studentIds = coursesStudentsMapper.findStudentIdByCourseId(courseId);
                obj.put("user_ids",studentIds);
                obj.put("msg_type","10");
                Map<String,String> extrasMap = new HashMap<>();
                extrasMap.put("msg_type","10");
                extrasMap.put("course_id",courseId);
                obj.put("extras_map", extrasMap);
                JPushHelper.push(obj);
                TimerTask timerTask = processCourseNotStartMap.remove(courseId);
                timerTask.cancel();
                timer.purge();
            }
        };

        processCourseNotStartMap.put(courseId, timerTask);
        long noticeTime= 3*60*1000;
        long taskTime = start_time - noticeTime;
        timer.schedule(timerTask, new Date(taskTime));
    }


    @FunctionName("processCourseStartLecturerNotShow")
    public void processCourseStartLecturerNotShow(RequestEntity requestEntity, JedisUtils jedisUtils, ApplicationContext context) {
        Jedis jedis = jedisUtils.getJedis();
        Map<String, Object> reqMap = (Map<String, Object>) requestEntity.getParam();
        log.debug("---------------将课程加入直播开始但是讲师未出现提醒定时任务"+reqMap);
        String courseId = reqMap.get("course_id").toString();
        String lecturer_id = reqMap.get("lecturer_id").toString();
        String course_title = reqMap.get("course_title").toString();
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {

                log.debug("-----------课程加入直播开始但是讲师未出现提醒定时任务 课程id"+courseId+"  执行时间"+System.currentTimeMillis());
                Map<String,Object> courseCacheMap = new HashMap<>();
                courseCacheMap.put(Constants.CACHED_KEY_COURSE_FIELD, courseId);
                String courseKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE, courseCacheMap);
                Map<String,String> courseMap = jedis.hgetAll(courseKey);
                //如果课程不是预告中，则不需要执行该定时任务
                if(courseMap == null || courseMap.size() == 0 || !courseMap.get("status").equals("1")){
                    TimerTask timerTask = processCourseNotStartMap.remove(courseId);
                    timerTask.cancel();
                    timer.purge();
                    return;
                }

                JSONObject obj = new JSONObject();
                obj.put("body",String.format(MiscUtils.getConfigByKey("jpush_course_start_lecturer_not_show"), course_title));
                obj.put("to",lecturer_id);
                obj.put("msg_type","3");
                Map<String,String> extrasMap = new HashMap<>();
                extrasMap.put("msg_type","3");
                extrasMap.put("course_id",courseId);
                obj.put("extras_map", extrasMap);
                JPushHelper.push(obj);
                TimerTask timerTask = processCourseNotStartMap.remove(courseId);
                timerTask.cancel();
                timer.purge();
            }
        };

        processCourseNotStartMap.put(courseId, timerTask);
        timer.schedule(timerTask, (Date)(reqMap.get("start_time")));
    }


    //开播预先24H提醒定时任务取消
    @FunctionName("processCourseStartLongNoticeCancel")
    public void processCourseStartLongNoticeCancel(RequestEntity requestEntity, JedisUtils jedisUtils, ApplicationContext context) {
        Map<String, Object> reqMap = (Map<String, Object>) requestEntity.getParam();
        String courseId = reqMap.get("course_id").toString();
        TimerTask timerTask = processCourseNotStartMap.remove(courseId);
        if(timerTask != null){
            timerTask.cancel();
            timer.purge();
        }
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

            //课程未开播强制结束
            if(type.equals("1")){
                //1.9极光推送结束消息
                JSONObject obj = new JSONObject();
                obj.put("body",String.format(MiscUtils.getConfigByKey("jpush_course_not_start_force_end"),courseMap.get("course_title")));
                obj.put("to",courseMap.get("lecturer_id"));
                obj.put("msg_type","6");
                Map<String,String> extrasMap = new HashMap<>();
                extrasMap.put("msg_type","6");
                extrasMap.put("course_id",courseId);
                obj.put("extras_map", extrasMap);
                JPushHelper.push(obj);
            }else if(type.equals("2")){
                //课程直播超时结束
                //1.9极光推送结束消息
                JSONObject obj = new JSONObject();
                obj.put("body",String.format(MiscUtils.getConfigByKey("jpush_course_live_overtime_force_end"),courseMap.get("course_title")));
                obj.put("to",courseMap.get("lecturer_id"));
                obj.put("msg_type","5");
                Map<String,String> extrasMap = new HashMap<>();
                extrasMap.put("msg_type","5");
                extrasMap.put("course_id",courseId);
                obj.put("extras_map", extrasMap);
                JPushHelper.push(obj);
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

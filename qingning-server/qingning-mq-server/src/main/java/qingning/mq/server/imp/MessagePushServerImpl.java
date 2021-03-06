package qingning.mq.server.imp;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import qingning.common.entity.QNLiveException;
import qingning.common.entity.RequestEntity;
import qingning.common.entity.TemplateData;
import qingning.common.util.*;
import qingning.db.common.mybatis.persistence.*;
import qingning.mq.server.entyity.QNQuartzSchedule;
import qingning.mq.server.entyity.QNSchedule;
import qingning.mq.server.entyity.ScheduleTask;
import qingning.server.AbstractMsgService;
import qingning.server.JedisBatchCallback;
import qingning.server.JedisBatchOperation;
import qingning.server.annotation.FunctionName;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;

import java.text.SimpleDateFormat;
import java.util.*;

public class MessagePushServerImpl extends AbstractMsgService {

    private static Logger log = LoggerFactory.getLogger(ImMsgServiceImp.class);
    @Autowired
    private CoursesMapper coursesMapper;

    @Autowired
    private CoursesStudentsMapper coursesStudentsMapper;

    @Autowired
    private CourseMessageMapper courseMessageMapper;

    @Autowired(required=true)
    private UserMapper userMapper;

    @Autowired(required=true)
    private LoginInfoMapper loginInfoMapper;

/*    private SaveCourseMessageService saveCourseMessageService;
    private SaveCourseAudioService saveCourseAudioService;*/

/*    static QNSchedule qnSchedule;
    static {
    	qnSchedule = new QNSchedule();
    }*/
    private static QNQuartzSchedule qnSchedule;
    static {
    	qnSchedule = new QNQuartzSchedule();
    }

/*    private SaveCourseMessageService getSaveCourseMessageService(ApplicationContext context){
    	if(saveCourseMessageService == null){
    		saveCourseMessageService =  (SaveCourseMessageService)context.getBean("SaveCourseMessageServer");
    	}
    	return saveCourseMessageService;
    }
    
    private SaveCourseAudioService getSaveCourseAudioService(ApplicationContext context){
    	if(saveCourseAudioService == null){
    		saveCourseAudioService =  (SaveCourseAudioService)context.getBean("SaveAudioMessageServer");
    	}
    	return saveCourseAudioService;
    }*/
    
//    //课程15分钟未开播，强制结束课程 type=1
//    @SuppressWarnings("unchecked")
//	@FunctionName("processCourseNotStart")
//    public void processForceEndCourse(RequestEntity requestEntity, JedisUtils jedisUtils, ApplicationContext context) {
////
////        Jedis jedis = jedisUtils.getJedis();
////        Map<String, Object> reqMap = (Map<String, Object>) requestEntity.getParam();
////        log.debug("---------------将课程加入未开播处理定时任务"+reqMap);
////        String courseId = reqMap.get("course_id").toString();
////
////        if(qnSchedule.containTask(courseId, QNSchedule.TASK_END_COURSE)){
////        	return;
////        }
////
////        long startTime = MiscUtils.convertObjectToLong(reqMap.get("start_time"));
////
////        //15分钟没出现会结束
////        String processCourseNotStartTime = IMMsgUtil.configMap.get("course_not_start_time_msec");
////        long taskStartTime = MiscUtils.convertObjectToLong(processCourseNotStartTime) + startTime;
////
////        ScheduleTask scheduleTask = new ScheduleTask(){
////			@Override
////			public void process() {
////                log.debug("-----------课程未开播处理定时任务 课程id"+this.getCourseId()+"  执行时间"+System.currentTimeMillis());
////                processCourseEnd(coursesMapper, "1", courseId, jedis);
////			}
////        };
////        scheduleTask.setId(courseId);
////        scheduleTask.setCourseId(courseId);
////        scheduleTask.setStartTime(taskStartTime);
////        scheduleTask.setTaskName(QNSchedule.TASK_END_COURSE);
////        qnSchedule.add(scheduleTask);
//    }

    //课程直播超时 极光推送
    @SuppressWarnings("unchecked")
	@FunctionName("processLiveCourseOvertimeNotice")
    public void processLiveCourseOvertimeNotice(RequestEntity requestEntity, JedisUtils jedisUtils, ApplicationContext context) {
        try{

            Jedis jedis = jedisUtils.getJedis();
            Map<String, Object> reqMap = (Map<String, Object>) requestEntity.getParam();
            log.debug("---------------将课程加入直播间即将超时预先提醒定时任务60分钟"+reqMap);
            String courseId = reqMap.get("course_id").toString();
            String im_course_id = reqMap.get("im_course_id").toString();
            String course_title = reqMap.get("course_title").toString();
            if(qnSchedule.containTask(courseId, QNSchedule.TASK_LECTURER_NOTICE)){
                return;
            }
            long real_start_time = MiscUtils.convertObjectToLong(reqMap.get("real_start_time"));

            //String courseOvertime = MiscUtils.getConfigByKey("course_live_overtime_msec");
            long taskStartTime = 5*60*60*1000 + real_start_time ;//上课到5小时
            log.debug("--------------超时任务处理时间5小时提醒任务,当前时间:"+System.currentTimeMillis()+"执行时间:"+taskStartTime);
            //taskStartTime -= 60*60*1000;//提前60分钟 提醒课程结束
            if(taskStartTime>0){
                ScheduleTask scheduleTask = new ScheduleTask(){
                    @Override
                    public void process() {
                        Map<String,Object> map = new HashMap<>();
                        map.put(Constants.CACHED_KEY_COURSE_FIELD, this.getCourseId());
                        String courseKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE, map);
                        Map<String,String> courseMap = jedis.hgetAll(courseKey);
                        if(MiscUtils.isEmpty(courseMap)  || "2".equals(courseMap.get("status"))){
                            return;
                        }
                        log.debug("-----------课程加入直播超时预先提醒定时任务 课程id"+courseId+"  执行时间"+System.currentTimeMillis());
                        JSONObject obj = new JSONObject();
                        obj.put("body",String.format(MiscUtils.getConfigByKey("jpush_course_live_overtime_per_notice_5h"), MiscUtils.RecoveryEmoji(course_title)));
                        obj.put("to",courseMap.get("lecturer_id"));
                        obj.put("msg_type","4");
                        Map<String,String> extrasMap = new HashMap<>();
                        extrasMap.put("msg_type","4");
                        extrasMap.put("course_id",courseId);
                        extrasMap.put("im_course_id",im_course_id);
                        obj.put("extras_map", extrasMap);
                        JPushHelper.push(obj);
                    }
                };
                scheduleTask.setId(courseId);
                scheduleTask.setCourseId(courseId);
                scheduleTask.setStartTime(taskStartTime);
                scheduleTask.setTaskName(QNSchedule.TASK_LECTURER_NOTICE);
                qnSchedule.add(scheduleTask);
            }
        }catch (Exception e){
            log.error("processLiveCourseOvertimeNotice 超时提醒出现异常");
        }
    }

    //课程直播超时处理 服务端逻辑
    @SuppressWarnings("unchecked")
    @FunctionName("processCourseLiveOvertime")
    public void processCourseLiveOvertime(RequestEntity requestEntity, JedisUtils jedisUtils, ApplicationContext context){
        try{

            Jedis jedis = jedisUtils.getJedis();
            Map<String, Object> reqMap = (Map<String, Object>) requestEntity.getParam();
            log.debug("---------------将课程加入直播超时处理定时任务"+reqMap);
            String courseId = reqMap.get("course_id").toString();
            if(qnSchedule.containTask(courseId, QNSchedule.TASK_END_COURSE)){
                return;
            }
            long realStartTime = MiscUtils.convertObjectToLong(reqMap.get("real_start_time"));//真实开课时间
            log.debug("---------------課程开课時間"+realStartTime);
            //6个小时 超时结束
            long taskStartTime = 6L*60L*60L*1000L + realStartTime;//加过6个小时的时间
            //long taskStartTime = 10*60*1000 + realStartTime;
            log.debug("--------------超时任务处理时间6小时,当前时间:"+System.currentTimeMillis()+"执行时间:"+taskStartTime);
            if(System.currentTimeMillis() - taskStartTime > 0){
                taskStartTime=System.currentTimeMillis()+10L*60L*1000L;
            }
            ScheduleTask scheduleTask = new ScheduleTask(){
                @Override
                public void process() {
                    log.debug("课程直播超时处理定时任务 课程id"+courseId+"  执行时间"+System.currentTimeMillis());
                    //processCourseEnd(coursesMapper,"2",courseId,jedis);
                    processSaveCourseImg(courseId,jedisUtils);
                }
            };
            scheduleTask.setId(courseId);
            scheduleTask.setCourseId(courseId);
            scheduleTask.setStartTime(taskStartTime);
            scheduleTask.setTaskName(QNSchedule.TASK_END_COURSE);
            qnSchedule.add(scheduleTask);

        }catch(Exception e){
            log.error("超时强制结束定时任务出现异常");
        }

    }

    //课程开始前24小时 极光推送给讲师
    @FunctionName("processCourseStartLongNotice")
    public void processCourseStartLongNotice(RequestEntity requestEntity, JedisUtils jedisUtils, ApplicationContext context) {
        try{
            @SuppressWarnings("unchecked")

            Map<String, Object> reqMap = (Map<String, Object>) requestEntity.getParam();
            log.debug("---------------将课程加入开播预先24H提醒定时任务"+reqMap);

            String courseId = reqMap.get("course_id").toString();
            if(qnSchedule.containTask(courseId, QNSchedule.TASK_COURSE_24H_NOTICE)){
                return;
            }
            String lecturer_id = reqMap.get("lecturer_id").toString();
            String course_title = reqMap.get("course_title").toString();
            String im_course_id = reqMap.get("im_course_id").toString();
            long start_time = MiscUtils.convertObjectToLong(reqMap.get("start_time"));

            //24小时 课程开始24小时时推送提示
            long noticeTime= 24L*60L*60L*1000L;
            //long noticeTime= 30*60*1000;
            long taskStartTime = start_time - noticeTime;
            if(taskStartTime>0){
                ScheduleTask scheduleTask = new ScheduleTask(){
                    @Override
                    public void process() {
                        log.debug("-----------开播预先24H提醒定时任务 课程id"+courseId+"  执行时间"+System.currentTimeMillis());
                        String fotmatString = "HH:mm";
                        String startTimeFormat = MiscUtils.parseDateToFotmatString((Date)(reqMap.get("start_time")),fotmatString);
                        JSONObject obj = new JSONObject();
                        obj.put("body",String.format(MiscUtils.getConfigByKey("jpush_course_start_per_long_notice"), MiscUtils.RecoveryEmoji(course_title),startTimeFormat));
                        obj.put("to",lecturer_id);
                        obj.put("msg_type","1");
                        Map<String,String> extrasMap = new HashMap<>();
                        extrasMap.put("msg_type","1");
                        extrasMap.put("course_id",courseId);
                        extrasMap.put("im_course_id",im_course_id);
                        obj.put("extras_map", extrasMap);
                        JPushHelper.push(obj);
                    }
                };
                scheduleTask.setId(courseId);
                scheduleTask.setCourseId(courseId);
                scheduleTask.setStartTime(taskStartTime);
                scheduleTask.setTaskName(QNSchedule.TASK_COURSE_24H_NOTICE);
                qnSchedule.add(scheduleTask);
            }
        }catch(Exception e){
            log.error("24小时前提醒出现异常");
        }

    }

    //课程上课提醒 课程开始前5分钟 提醒老师
    @FunctionName("processCourseStartShortNotice")
    public void processCourseStartShortNotice(RequestEntity requestEntity, JedisUtils jedisUtils, ApplicationContext context) {

        try{
            @SuppressWarnings("unchecked")

            Map<String, Object> reqMap = (Map<String, Object>) requestEntity.getParam();
            log.debug("---------------将课程加入开播预先5分钟提醒定时任务"+reqMap);
            String courseId = reqMap.get("course_id").toString();
            if(qnSchedule.containTask(courseId, QNSchedule.TASK_COURSE_5MIN_NOTICE)){
                return;
            }
            String lecturer_id = reqMap.get("lecturer_id").toString();
            String course_title = reqMap.get("course_title").toString();
            String im_course_id = reqMap.get("im_course_id").toString();
            long start_time = MiscUtils.convertObjectToLong(reqMap.get("start_time"));

            //提前5分钟
            long noticeTime= 5*60*1000;
            long taskStartTime = start_time - noticeTime;
            if(taskStartTime>0){
                ScheduleTask scheduleTask = new ScheduleTask(){
                    @Override
                    public void process() {
                        log.debug("-----------开播预先5min提醒定时任务 课程id"+courseId+"  执行时间"+System.currentTimeMillis());
                        JSONObject obj = new JSONObject();
                        obj.put("body",String.format(MiscUtils.getConfigByKey("jpush_course_start_per_short_notice"), MiscUtils.RecoveryEmoji(course_title),"5"));
                        obj.put("to",lecturer_id);
                        obj.put("msg_type","2");
                        Map<String,String> extrasMap = new HashMap<>();
                        extrasMap.put("msg_type","2");
                        extrasMap.put("course_id",courseId);
                        extrasMap.put("im_course_id",im_course_id);
                        obj.put("extras_map", extrasMap);
                        JPushHelper.push(obj);
//
//                        Map<String,String> map = new HashMap<>();
//                        map.put("course_id", courseId);
//                        String sys_course_guest =  MiscUtils.getKeyOfCachedData(Constants.SYS_COURSE_GUEST, map);

//                        Jedis jedis = jedisUtils.getJedis();
//                        Set<String> guestIds = jedis.zrangeByScore(sys_course_guest,"-inf","+inf");
//                        for(String guest_user_id : guestIds){
//                            JSONObject obj1 = new JSONObject();
//                            obj1.put("body",String.format(MiscUtils.getConfigByKey("jpush_course_start_per_short_notice"), MiscUtils.RecoveryEmoji(course_title),"5"));
//                            obj1.put("to",guest_user_id);
//                            obj1.put("msg_type","2");
//                            Map<String,String> extrasMap1 = new HashMap<>();
//                            extrasMap1.put("msg_type","2");
//                            extrasMap1.put("course_id",courseId);
//                            extrasMap1.put("im_course_id",im_course_id);
//                            obj1.put("extras_map", extrasMap);
//                            JPushHelper.push(obj1);
//                        }


                    }
                };
                scheduleTask.setId(courseId);
                scheduleTask.setCourseId(courseId);
                scheduleTask.setStartTime(taskStartTime);
                scheduleTask.setTaskName(QNSchedule.TASK_COURSE_5MIN_NOTICE);
                qnSchedule.add(scheduleTask);
            }
        }catch(Exception e){
            log.error("提前5分钟提醒任务设置出现异常");
        }


    }

//
//    //课程开始前三分钟 极光推送 提醒学生学习
//    @SuppressWarnings("unchecked")
//	@FunctionName("processCourseStartStudentStudyNotice")
//    public void processCourseStartStudentStudyNotice(RequestEntity requestEntity, JedisUtils jedisUtils, ApplicationContext context) {
////
////        Map<String, Object> reqMap = (Map<String, Object>) requestEntity.getParam();
////        log.debug("---------------加入学生上课3min提醒定时任务"+reqMap);
////        String courseId = reqMap.get("course_id").toString();
////        String im_course_id = reqMap.get("im_course_id").toString();
////        if(qnSchedule.containTask(courseId, QNSchedule.TASK_COURSE_15MIN_NOTICE)){
////        	return;
////        }
////        String course_title = reqMap.get("course_title").toString();
////        long start_time = MiscUtils.convertObjectToLong(reqMap.get("start_time"));
////        SimpleDateFormat sdf =   new SimpleDateFormat("yyyy年MM月dd日HH:mm");
////        String str = sdf.format(start_time);
////
////        //3分钟
////        long noticeTime= 3*60*1000;
////        long taskStartTime = start_time - noticeTime;
////        if(taskStartTime > 0){
////        	ScheduleTask scheduleTask = new ScheduleTask(){
////        		@Override
////        		public void process() {
////                    log.debug("-----------加入学生上课3min提醒定时任务 课程id"+courseId+"  执行时间"+System.currentTimeMillis());
////                    JSONObject obj = new JSONObject();
////                    obj.put("body",String.format(MiscUtils.getConfigByKey("jpush_course_study_notice"), MiscUtils.RecoveryEmoji(course_title)));
////                    List<String> studentIds = coursesStudentsMapper.findUserIdsByCourseId(courseId);
////                    if(MiscUtils.isEmpty(studentIds)){
////                    	return;
////                    }
////                    obj.put("user_ids",studentIds);
////                    obj.put("msg_type","10");
////                    Map<String,String> extrasMap = new HashMap<>();
////                    extrasMap.put("msg_type","10");
////                    extrasMap.put("course_id",courseId);
////                    extrasMap.put("im_course_id", im_course_id);
////                    obj.put("extras_map", extrasMap);
////                    JPushHelper.push(obj);
////
////
//////                    Map<String, TemplateData> templateMap = new HashMap<String, TemplateData>();
//////                    TemplateData first = new TemplateData();
//////                    first.setColor(Constants.WE_CHAT_PUSH_COLOR);
//////                    first.setValue(MiscUtils.getConfigByKey("wpush_start_lesson_first"));
//////                    templateMap.put("first", first);
//////
//////                    TemplateData orderNo = new TemplateData();
//////                    orderNo.setColor(Constants.WE_CHAT_PUSH_COLOR);
//////                    orderNo.setValue(MiscUtils.RecoveryEmoji(course_title));
//////                    templateMap.put("keyword1", orderNo);
//////
//////                    TemplateData wuliu = new TemplateData();
//////                    wuliu.setColor(Constants.WE_CHAT_PUSH_COLOR);
//////                    wuliu.setValue(str);
//////                    templateMap.put("keyword2", wuliu);
//////
//////                    TemplateData remark = new TemplateData();
//////                    remark.setColor(Constants.WE_CHAT_PUSH_COLOR);
//////                    remark.setValue(MiscUtils.getConfigByKey("wpush_start_lesson_remark"));
//////                    templateMap.put("remark", remark);
//////
//////                    if (studentIds!=null && studentIds.size()>0) {
//////                        String url=String.format(MiscUtils.getConfigByKey("course_live_room_url"), courseId,roomId);
//////                        Jedis jedis = jedisUtils.getJedis();
//////                        weiPush(studentIds, MiscUtils.getConfigByKey("wpush_start_lesson"),url,templateMap, jedis);
//////                    }
////
////        		}
////        	};
////        	scheduleTask.setId(courseId);
////            scheduleTask.setCourseId(courseId);
////            scheduleTask.setStartTime(taskStartTime);
////            scheduleTask.setTaskName(QNSchedule.TASK_COURSE_15MIN_NOTICE);
////            qnSchedule.add(scheduleTask);
////        }
//    }

    //课程开始讲师未出现 极光推送
    @SuppressWarnings("unchecked")
	@FunctionName("processCourseStartLecturerNotShow")
    public void processCourseStartLecturerNotShow(RequestEntity requestEntity, JedisUtils jedisUtils, ApplicationContext context) {
        try{

            Jedis jedis = jedisUtils.getJedis();
            Map<String, Object> reqMap = (Map<String, Object>) requestEntity.getParam();
            log.debug("---------------将课程加入直播开始但是讲师未出现提醒定时任务"+reqMap);
            String courseId = reqMap.get("course_id").toString();
            if(qnSchedule.containTask(courseId, QNSchedule.TASK_LECTURER_NOTICE)){
                return;
            }
            String lecturer_id = reqMap.get("lecturer_id").toString();
            String course_title = reqMap.get("course_title").toString();
            String im_course_id = reqMap.get("im_course_id").toString();

            //课程开始时间
            long taskStartTime = MiscUtils.convertObjectToLong(reqMap.get("start_time"));

            if(taskStartTime > 0){
                ScheduleTask scheduleTask = new ScheduleTask(){
                    @Override
                    public void process() {
                        log.debug("-----------课程加入直播开始但是讲师未出现提醒定时任务 课程id"+courseId+"  执行时间"+System.currentTimeMillis());
                        Map<String,Object> courseCacheMap = new HashMap<>();
                        courseCacheMap.put(Constants.CACHED_KEY_COURSE_FIELD, courseId);
                        String courseKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE, courseCacheMap);
                        Map<String,String> courseMap = jedis.hgetAll(courseKey);
                        //如果课程不是预告中，则不需要执行该定时任务
                        if(courseMap == null || courseMap.size() == 0){
                            return;
                        }
                        JSONObject obj = new JSONObject();
                        obj.put("body",String.format(MiscUtils.getConfigByKey("jpush_course_start_lecturer_not_show"), MiscUtils.RecoveryEmoji(course_title)));
                        obj.put("to",lecturer_id);
                        obj.put("msg_type","3");
                        Map<String,String> extrasMap = new HashMap<>();
                        extrasMap.put("msg_type","3");
                        extrasMap.put("course_id",courseId);
                        extrasMap.put("im_course_id",im_course_id);
                        obj.put("extras_map", extrasMap);
                        JPushHelper.push(obj);

                    }
                };
                scheduleTask.setId(courseId);
                scheduleTask.setCourseId(courseId);
                scheduleTask.setLecturerId(lecturer_id);
                scheduleTask.setStartTime(taskStartTime);
                scheduleTask.setTaskName(QNSchedule.TASK_LECTURER_NOTICE);
                qnSchedule.add(scheduleTask);
            }
        }catch(Exception e){
            log.error("老师未出现提醒出现异常");
        }


    }

    //IM推送
    @SuppressWarnings("unchecked")
    @FunctionName("processCourseStartIM")
    public void processCourseStartIM(RequestEntity requestEntity, JedisUtils jedisUtils, ApplicationContext context) {
        try{
            log.debug("---------------将课程加入发送上课开始的im消息");

            Jedis jedis = jedisUtils.getJedis();
            Map<String, Object> reqMap = (Map<String, Object>) requestEntity.getParam();
            String courseId = reqMap.get("course_id").toString();
            if(qnSchedule.containTask(courseId, QNSchedule.TASK_COURSE_START)){
                log.debug("------------已经有任务了:"+QNSchedule.TASK_COURSE_START+"course_id:"+courseId);
                return;
            }
            //课程开始时间
            long taskStartTime = MiscUtils.convertObjectToLong(reqMap.get("start_time"));
            String lecturer_id = reqMap.get("lecturer_id").toString();

            if(taskStartTime > 0){
                ScheduleTask scheduleTask = new ScheduleTask(){
                    @Override
                    public void process() {
                        log.debug("----------发送上课开始的im消息 课程id"+courseId+"  执行时间"+System.currentTimeMillis());
                        Map<String,Object> courseCacheMap = new HashMap<>();
                        courseCacheMap.put(Constants.CACHED_KEY_COURSE_FIELD, courseId);
                        String courseKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE, courseCacheMap);
                        Map<String,String> courseMap = jedis.hgetAll(courseKey);
                        ////发送结束推送消息
                        long currentTime = System.currentTimeMillis();
                        String mGroupId = jedis.hget(courseKey,"im_course_id");
                        String sender = "system";
                        Map<String,Object> infomation = new HashMap<>();
                        infomation.put("course_id", courseId);
                        infomation.put("creator_id", courseMap.get("lecturer_id"));
                        infomation.put("message",  MiscUtils.getConfigByKey("course_start_msg"));
                        infomation.put("message_id",MiscUtils.getUUId());
                        infomation.put("message_imid",infomation.get("message_id"));
                        infomation.put("message_type", "1");
                        infomation.put("send_type", "5");//5开始
                        infomation.put("create_time", currentTime);
                        Map<String,Object> messageMap = new HashMap<>();
                        messageMap.put("msg_type","1");
                        messageMap.put("send_time",currentTime);
                        messageMap.put("information",infomation);
                        messageMap.put("mid",infomation.get("message_id"));
                        String content = JSON.toJSONString(messageMap);
                        IMMsgUtil.sendMessageInIM(mGroupId, content, "", sender);

                        jedis.zrem(Constants.SYS_COURSES_RECOMMEND_PREDICTION,courseId);//从热门推荐预告列表删除
                        long lops = Long.valueOf(courseMap.get("student_num"))+ Long.valueOf(courseMap.get("extra_num"));
                        jedis.zadd(Constants.SYS_COURSES_RECOMMEND_LIVE,lops,courseId);//加入热门推荐正在直播列表

                        log.debug("----------发送上课开始的极光消息 课程id"+courseId+"  执行时间"+System.currentTimeMillis());
                        SimpleDateFormat sdf =   new SimpleDateFormat("yyyy年MM月dd日HH:mm");
                        String str = sdf.format( System.currentTimeMillis());
                        Map<String, TemplateData> templateMap = new HashMap<String, TemplateData>();
                        TemplateData first = new TemplateData();
                        first.setColor(Constants.WE_CHAT_PUSH_COLOR);
                        first.setValue(MiscUtils.getConfigByKey("wpush_start_lesson_first"));
                        templateMap.put("first", first);

                        TemplateData orderNo = new TemplateData();
                        orderNo.setColor(Constants.WE_CHAT_PUSH_COLOR);
                        orderNo.setValue(MiscUtils.RecoveryEmoji(courseMap.get("course_title")));
                        templateMap.put("keyword1", orderNo);

                        TemplateData wuliu = new TemplateData();
                        wuliu.setColor(Constants.WE_CHAT_PUSH_COLOR);
                        wuliu.setValue(str);
                        templateMap.put("keyword2", wuliu);

                        TemplateData remark = new TemplateData();
                //       if(.equals(Constants.HEADER_APP_NAME)){
                            remark.setColor(Constants.WE_CHAT_PUSH_COLOR_QNCOLOR);
//                        }else{
//                            remark.setColor(Constants.WE_CHAT_PUSH_COLOR_DLIVE);
//                        }
                        remark.setValue(MiscUtils.getConfigByKey("wpush_start_lesson_remark"));
                        templateMap.put("remark", remark);
                        //查询报名了的用户id
                        List<String> findFollowUserIds =  coursesStudentsMapper.findUserIdsByCourseId(courseMap.get("course_id"));

                        String url = MiscUtils.getConfigByKey("course_live_room_url");
                        url=String.format(url,  courseMap.get("course_id"),courseMap.get("room_id"));
                        if (findFollowUserIds!=null && findFollowUserIds.size()>0) {
                            weiPush(findFollowUserIds, MiscUtils.getConfigByKey("wpush_start_lesson"),url,templateMap, jedis);
                        }
                    }
                };
                scheduleTask.setId(courseId);
                scheduleTask.setCourseId(courseId);
                scheduleTask.setLecturerId(lecturer_id);
                scheduleTask.setStartTime(taskStartTime);
                scheduleTask.setTaskName(QNSchedule.TASK_COURSE_START);
                qnSchedule.add(scheduleTask);
                log.debug("---------------将课程加入发送上课开始的im消息成功"+reqMap);
            }
        }catch(Exception e){
            log.error("设置IM 出现异常");
        }


    }


    /**
     *
     */
    private void processSaveCourseImg(String course_id,JedisUtils jedisUtils){
        Map<String, Object> map = new HashMap<>();
        map.put(Constants.CACHED_KEY_COURSE_FIELD, course_id);
        String messageListKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_MESSAGE_LIST, map);//COURSE:{course_id}:MESSAGE_LIST
        Jedis jedis = jedisUtils.getJedis();
        String courseKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE, map);//COURSE:{course_id}:MESSAGE_LIST
        Map<String, String> courseMap = jedis.hgetAll(courseKey);
        String lecturer_id = courseMap.get("lecturer_id");

        Date courseEndTime = new Date();//结束时间
        map.put("now",courseEndTime);
        map.put("status","2");
        int update_count = coursesMapper.updateCourse(map);
        if (update_count != 0) {


        //1.3将该课程从讲师的预告课程列表 SYS: lecturer:{ lecturer_id }：courses  ：prediction移动到结束课程列表 SYS: lecturer:{ lecturer_id }：courses  ：finish
        map.put(Constants.CACHED_KEY_LECTURER_FIELD, lecturer_id);
        String lecturerCoursesPredictionKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_PREDICTION, map);
        jedis.zrem(lecturerCoursesPredictionKey, course_id);

        long lpos = MiscUtils.convertInfoToPostion(courseEndTime.getTime(), MiscUtils.convertObjectToLong(jedis.hget(courseKey, "position")));

        String lecturerCoursesFinishKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_FINISH, map);

        jedis.zadd(lecturerCoursesFinishKey, lpos,course_id);

        //1.4将该课程从平台的预告课程列表 SYS：courses  ：prediction移除。如果存在结束课程列表 SYS：courses ：finish，则增加到课程结束列表
        jedis.zrem(Constants.CACHED_KEY_PLATFORM_COURSE_PREDICTION, course_id);
        jedis.zadd(Constants.CACHED_KEY_PLATFORM_COURSE_FINISH, lpos,course_id);

        jedis.zrem(Constants.SYS_COURSES_RECOMMEND_LIVE,course_id);//从热门推荐正在直播列表删除
        long lops = Long.valueOf(courseMap.get("student_num"))+ Long.valueOf(courseMap.get("extra_num"));
        jedis.zadd(Constants.SYS_COURSES_RECOMMEND_FINISH,lops,course_id);//加入热门推荐结束列表


        map.put(Constants.CACHED_KEY_CLASSIFY,courseMap.get("classify_id"));
        jedis.zrem(MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_PLATFORM_COURSE_CLASSIFY_PREDICTION, map), course_id);//删除预告
        jedis.zadd(MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_PLATFORM_COURSE_CLASSIFY_FINISH, map), lpos, course_id);//在结束中增加

        //1.5如果课程标记为结束，则清除该课程的禁言缓存数据
        map.put(Constants.CACHED_KEY_COURSE_FIELD, course_id);
        String banKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_BAN_USER_LIST, map);
        jedis.del(banKey);
        //1.6更新课程缓存信息
        Map<String, String> updateCacheMap = new HashMap<String, String>();
        updateCacheMap.put("update_time", System.currentTimeMillis() + "");
        updateCacheMap.put("end_time", System.currentTimeMillis()+ "");
        updateCacheMap.put("status", "2");
        jedis.hmset(courseKey, updateCacheMap);


        //1.9如果该课程没有真正开播，并且开播时间在今天之内，则需要取消课程超时未开播定时任务
        if(jedis.hget(courseKey, "real_start_time") == null){
            this.processCourseNotStartCancel(course_id);
        }

        courseMap.put("update_time", courseEndTime.getTime()+"");
        String mGroupId = jedis.hget(courseKey,"im_course_id");
        Map<String, Object> userInfo = userMapper.findByUserId(lecturer_id);
        Map<String,Object> startLecturerMessageInformation = new HashMap<>();
        String imid = MiscUtils.getUUId();
        startLecturerMessageInformation.put("creator_id",lecturer_id);//发送人id
        startLecturerMessageInformation.put("course_id", course_id);//课程id
        startLecturerMessageInformation.put("message",MiscUtils.getConfigByKey("end_lecturer_message"));
        startLecturerMessageInformation.put("message_type", "1");
        startLecturerMessageInformation.put("message_id",imid);
        startLecturerMessageInformation.put("message_imid",imid);
        startLecturerMessageInformation.put("create_time",  System.currentTimeMillis());
        startLecturerMessageInformation.put("send_type","0");
        startLecturerMessageInformation.put("creator_avatar_address",userInfo.get("avatar_address"));
        startLecturerMessageInformation.put("creator_nick_name",userInfo.get("nick_name"));
        Map<String,Object> startLecturerMessageMap = new HashMap<>();
        startLecturerMessageMap.put("msg_type","1");
        startLecturerMessageMap.put("send_time", System.currentTimeMillis());
        startLecturerMessageMap.put("create_time", System.currentTimeMillis());
        startLecturerMessageMap.put("information",startLecturerMessageInformation);
        startLecturerMessageMap.put("mid",imid);
        String startLecturerMessageInformationContent = JSON.toJSONString(startLecturerMessageMap);
        IMMsgUtil.sendMessageInIM(mGroupId, startLecturerMessageInformationContent, "", loginInfoMapper.findLoginInfoByUserId(lecturer_id).get("m_user_id").toString());//发送信息
        messageListKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_MESSAGE_LIST, startLecturerMessageInformation);
//					//1.将聊天信息id插入到redis zsort列表中
        jedis.zadd(messageListKey,  System.currentTimeMillis(), (String)startLecturerMessageInformation.get("message_imid"));
//					//添加到老师发送的集合中
        String messageLecturerListKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_MESSAGE_LIST_LECTURER, map);
        jedis.zadd(messageLecturerListKey,  System.currentTimeMillis(),startLecturerMessageInformation.get("message_imid").toString());

        String messageKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_MESSAGE, startLecturerMessageInformation);//直播间
        Map<String,String> result = new HashMap<String,String>();
        MiscUtils.converObjectMapToStringMap(startLecturerMessageInformation, result);
        jedis.hmset(messageKey, result);
        SimpleDateFormat sdf =   new SimpleDateFormat("yyyy年MM月dd日HH:mm");
        String str = sdf.format(courseEndTime);
        String courseEndMessage = "直播结束于"+str;
        //发送结束推送消息
        long currentTime = System.currentTimeMillis();
        String message = courseEndMessage;
        String sender = "system";
        Map<String,Object> infomation = new HashMap<>();
        infomation.put("course_id",course_id);
        infomation.put("creator_id", lecturer_id);
        infomation.put("message", message);
        infomation.put("message_type", "1");
        infomation.put("send_type", "6");//5.结束消息
        infomation.put("message_id",MiscUtils.getUUId());
        infomation.put("message_imid",infomation.get("message_id"));
        infomation.put("create_time", currentTime);
        Map<String,Object> messageMap = new HashMap<>();
        messageMap.put("msg_type","1");
        messageMap.put("send_time", System.currentTimeMillis());
        messageMap.put("create_time", System.currentTimeMillis());
        messageMap.put("information",infomation);
        messageMap.put("mid",infomation.get("message_id"));
        String content = JSON.toJSONString(messageMap);
        IMMsgUtil.sendMessageInIM(mGroupId, content, "", sender);


        //1.从缓存中查询该课程的消息列表
        Set<String> messageIdList = jedis.zrange(messageListKey, 0, -1);//jedisObject.zrevrange(messageListKey, 0 , -1);
        if(!MiscUtils.isEmpty(messageIdList)){
            //2.批量从缓存中读取消息详细信息
            List<Map<String,Object>> messageList = new ArrayList<>();
            List<String> messageKeyList = new ArrayList<>();
            JedisBatchCallback callBack = (JedisBatchCallback)jedisUtils.getJedis();
            callBack.invoke(new JedisBatchOperation(){
                @Override
                public void batchOperation(Pipeline pipeline, Jedis jedis) {

                    long messagePos = 0L;
                    List<Response<Map<String, String>>> redisResponseList = new ArrayList<>();
                    for(String messageimid : messageIdList){
                        map.put(Constants.FIELD_MESSAGE_ID, messageimid);
                        String messageKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_MESSAGE, map);
                        redisResponseList.add(pipeline.hgetAll(messageKey));
                        messageKeyList.add(messageKey);
                    }
                    pipeline.sync();

                    for(Response<Map<String, String>> redisResponse : redisResponseList){
                        Map<String,String> messageStringMap = redisResponse.get();
                        Map<String,Object> messageObjectMap = new HashMap<>();
                        if(messageStringMap.get("message_imid") == null){
                            return;
                        }
                        if(!MiscUtils.isEmpty(messageStringMap.get("message_id"))){
                            messageObjectMap.put("message_id", messageStringMap.get("message_id"));
                        }

                        messageObjectMap.put("course_id", messageStringMap.get("course_id"));


                        if(!MiscUtils.isEmpty(messageStringMap.get("message"))){
                            messageObjectMap.put("message", messageStringMap.get("message"));
                        }else{
                            messageObjectMap.put("message", null);
                        }

                        if(!MiscUtils.isEmpty(messageStringMap.get("message_url"))){
                            messageObjectMap.put("message_url", messageStringMap.get("message_url"));
                        }else{
                            messageObjectMap.put("message_url",null);
                        }

                        if(!MiscUtils.isEmpty(messageStringMap.get("message_question"))){
                            messageObjectMap.put("message_question", messageStringMap.get("message_question"));
                        }else{
                            messageObjectMap.put("message_question", null);
                        }


                        if(!MiscUtils.isEmpty(messageStringMap.get("audio_time"))){
                            messageObjectMap.put("audio_time", Long.parseLong(messageStringMap.get("audio_time")));
                        }else {
                            messageObjectMap.put("audio_time", 0);
                        }


                        messageObjectMap.put("message_type", messageStringMap.get("message_type"));
                        messageObjectMap.put("send_type", messageStringMap.get("send_type"));


                        messageObjectMap.put("creator_id", messageStringMap.get("creator_id"));

                        if(!MiscUtils.isEmpty(messageStringMap.get("create_time"))){
                            Date createTime = new Date(Long.parseLong(messageStringMap.get("create_time")));
                            messageObjectMap.put("create_time", createTime);
                        }
                        if(!MiscUtils.isEmpty(messageStringMap.get("audio_image"))){
                            messageObjectMap.put("audio_image", messageStringMap.get("audio_image"));
                        }else{
                            messageObjectMap.put("audio_image", null);
                        }
                        if(!MiscUtils.isEmpty(messageStringMap.get("message_status"))){
                            messageObjectMap.put("message_status",messageStringMap.get("message_status"));
                        }else{
                            messageObjectMap.put("message_status",0);
                        }
                        if(!MiscUtils.isEmpty(messageStringMap.get("message_imid"))){
                            messageObjectMap.put("message_imid", messageStringMap.get("message_imid"));
                        }else{
                            messageObjectMap.put("message_imid",MiscUtils.getUUId());
                        }

                        messageObjectMap.put("message_pos", messagePos++);
                        messageList.add(messageObjectMap);
                    }
                }
            });
            //3.批量插入到数据库中
            Integer insertResult = courseMessageMapper.insertCourseMessageList(messageList);

            //4.如果插入数据库正常，则删除缓存中的内容
            if(insertResult != null && insertResult > 0){
                //删除redis中的key
                String[] messageKeyArray = new String[messageKeyList.size()];
                messageKeyList.toArray(messageKeyArray);
                jedis.del(messageKeyArray);
                jedis.del(messageListKey);
                String messageUserListKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_MESSAGE_LIST_USER, map);
                String messageLecturerVoiceListKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_MESSAGE_LIST_LECTURER_VOICE, map);
                jedis.del(messageUserListKey);
                jedis.del(messageLecturerListKey);
                jedis.del(messageLecturerVoiceListKey);
            }
        }
        }
    }




    //开播预先24H提醒定时任务取消
    @SuppressWarnings("unchecked")
	@FunctionName("processCourseStartLongNoticeCancel")
    public void processCourseStartLongNoticeCancel(RequestEntity requestEntity, JedisUtils jedisUtils, ApplicationContext context) {

        Map<String, Object> reqMap = (Map<String, Object>) requestEntity.getParam();
        String courseId = reqMap.get("course_id").toString();        
        qnSchedule.cancelTask(courseId, QNSchedule.TASK_COURSE_24H_NOTICE);
    }

    @SuppressWarnings("unchecked")
    @FunctionName("processCourseStartLongNoticeUpdate")
    public void processCourseStartLongNoticeUpdate(RequestEntity requestEntity, JedisUtils jedisUtils, ApplicationContext context) {
        Map<String, Object> reqMap = (Map<String, Object>) requestEntity.getParam();

        long startTime = MiscUtils.convertObjectToLong(reqMap.get("start_time"));
        String courseId = (String)reqMap.get("course_id");
        qnSchedule.cancelTask(courseId, QNSchedule.TASK_COURSE_24H_NOTICE);

        if(MiscUtils.isTheSameDate(new Date(startTime- 60 * 60 *1000*24), new Date()) && startTime-System.currentTimeMillis()> 60 * 60 *1000*24){
            processCourseStartLongNotice(requestEntity, jedisUtils, context);
        }
    }

    @FunctionName("processCourseStartShortNoticeCancel")
    public void processCourseStartShortNoticeCancel(RequestEntity requestEntity, JedisUtils jedisUtils, ApplicationContext context) {
        processCourseStartShortNoticeUpdate(requestEntity, jedisUtils, context, false);

    }

    @FunctionName("processCourseStartShortNoticeUpdate")
    public void processCourseStartShortNoticeUpdate(RequestEntity requestEntity, JedisUtils jedisUtils, ApplicationContext context) {
        processCourseStartShortNoticeUpdate(requestEntity, jedisUtils, context, true);
    }

    @SuppressWarnings("unchecked")
    private void processCourseStartShortNoticeUpdate(RequestEntity requestEntity, JedisUtils jedisUtils, ApplicationContext context, boolean update) {
        Map<String, Object> reqMap = (Map<String, Object>) requestEntity.getParam();
        long startTime = MiscUtils.convertObjectToLong(reqMap.get("start_time"));
        String courseId = (String)reqMap.get("course_id");
        qnSchedule.cancelTask(courseId, QNSchedule.TASK_COURSE_5MIN_NOTICE);
        if(update && MiscUtils.isTheSameDate(new Date(startTime), new Date())){
            if(startTime-System.currentTimeMillis()> 5 * 60 *1000){
                this.processCourseStartShortNotice(requestEntity, jedisUtils, context);
            }
        }
    }

    //课程未开播处理定时任务取消
    @SuppressWarnings("unchecked")
	@FunctionName("processCourseNotStartCancel")
    public void processCourseNotStartCancel(RequestEntity requestEntity, JedisUtils jedisUtils, ApplicationContext context) {
        Map<String, Object> reqMap = (Map<String, Object>) requestEntity.getParam();
        String courseId = reqMap.get("course_id").toString();
        qnSchedule.cancelTask(courseId, QNSchedule.TASK_END_COURSE);
    }

    public void processCourseNotStartCancel(String course_id) {
        qnSchedule.cancelTask(course_id, QNSchedule.TASK_END_COURSE);
    }


    //课程未开播处理定时任务取消
    @SuppressWarnings("unchecked")
    @FunctionName("processCourseNotStartCancelAll")
    public void processCourseNotStartCancelAll(RequestEntity requestEntity, JedisUtils jedisUtils, ApplicationContext context) {
        Map<String, Object> reqMap = (Map<String, Object>) requestEntity.getParam();
        String courseId = reqMap.get("course_id").toString();
        qnSchedule.cancelTask(courseId, QNSchedule.TASK_END_COURSE);
        qnSchedule.cancelTask(courseId, QNSchedule.TASK_COURSE_OVER_TIME_NOTICE);
        qnSchedule.cancelTask(courseId, QNSchedule.TASK_COURSE_24H_NOTICE);
        qnSchedule.cancelTask(courseId, QNSchedule.TASK_COURSE_5MIN_NOTICE);
        qnSchedule.cancelTask(courseId, QNSchedule.TASK_COURSE_15MIN_NOTICE);
        qnSchedule.cancelTask(courseId, QNSchedule.TASK_LECTURER_NOTICE);
        qnSchedule.cancelTask(courseId, QNSchedule.TASK_COURSE_START);
    }




    @SuppressWarnings("unchecked")
    @FunctionName("processCourseStartLecturerNotShowUpdate")
    public void processCourseStartLecturerNotShowUpdate(RequestEntity requestEntity, JedisUtils jedisUtils, ApplicationContext context) {
        Map<String, Object> reqMap = (Map<String, Object>) requestEntity.getParam();
        long startTime = MiscUtils.convertObjectToLong(reqMap.get("start_time"));
        String courseId = (String)reqMap.get("course_id");
        qnSchedule.cancelTask(courseId, QNSchedule.TASK_LECTURER_NOTICE);

        if(MiscUtils.isTheSameDate(new Date(startTime), new Date())){
            if(startTime > System.currentTimeMillis()){
                processCourseStartLecturerNotShow(requestEntity, jedisUtils, context);
            }
        }
    }



    //type 为1则为课程未开播强制结束，type为2则为课程直播超时强制结束
    public void processCourseEnd(CoursesMapper processCoursesMapper, String type ,String courseId, Jedis jedis){

        log.debug("-----------課程强制结束任务 course_id:"+courseId+" 执行类型:"+ type +" 执行时间"+System.currentTimeMillis());

        Map<String,Object> map = new HashMap<>();
        map.put(Constants.CACHED_KEY_COURSE_FIELD, courseId);
        String courseKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE, map);
        Map<String,String> courseMap = jedis.hgetAll(courseKey);
        long realStartTime = MiscUtils.convertObjectToLong(courseMap.get("real_start_time"));

        boolean processFlag = false;
        if(type.equals("2")){
            if(! courseMap.get("status").equals("2")){
                processFlag = true;
            }
        }

        //如果为未开播课程，则对课程进行结束处理
        if(processFlag){
            //1.1如果为课程结束，则取当前时间为课程结束时间
            Date now = new Date();
            //1.2更新课程详细信息（变更课程为已经结束）
            Map<String,Object> course = new HashMap<String,Object>();
            course.put("course_id", courseId);
            course.put("end_time", now);
            course.put("update_time", now);
            course.put("status", "2");           
            processCoursesMapper.updateCourse(course);

            //1.3将该课程从讲师的预告课程列表 SYS: lecturer:{ lecturer_id }：courses  ：prediction移动到结束课程列表 SYS: lecturer:{ lecturer_id }：courses  ：finish
            map.clear();
            map.put(Constants.CACHED_KEY_LECTURER_FIELD, courseMap.get("lecturer_id"));

            String lecturerCoursesPredictionKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_PREDICTION, map);
            jedis.zrem(lecturerCoursesPredictionKey, courseId);

            String lecturerCoursesFinishKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_FINISH, map);
            long  position = MiscUtils.convertObjectToLong(jedis.hget(courseKey, "position"));
            jedis.zadd(lecturerCoursesFinishKey, MiscUtils.convertInfoToPostion(now.getTime(), position), courseId);

            //1.4将该课程从平台的预告课程列表 SYS：courses  ：prediction移除。如果存在结束课程列表 SYS：courses ：finish，则增加到课程结束列表
            jedis.zrem(Constants.CACHED_KEY_PLATFORM_COURSE_PREDICTION, courseId);
            if(jedis.exists(Constants.CACHED_KEY_PLATFORM_COURSE_FINISH)){            	
                jedis.zadd(Constants.CACHED_KEY_PLATFORM_COURSE_FINISH, MiscUtils.convertInfoToPostion( now.getTime(),position), courseId);
            }

            jedis.zrem(Constants.SYS_COURSES_RECOMMEND_LIVE,courseId);//从热门推荐正在直播列表删除
            long lops = Long.valueOf(courseMap.get("student_num"))+ Long.valueOf(courseMap.get("extra_num"));
            jedis.zadd(Constants.SYS_COURSES_RECOMMEND_FINISH,lops,courseId);//加入热门推荐结束列表

            //将该课程从分类预告课程列表 SYS:COURSES:{classify_id}:PREDICTION 移除. 如果存在判断当前课程是否存在 加入结束列表中 SYS:COURSES:{classify_id}:FINISH
            map.put(Constants.CACHED_KEY_CLASSIFY,courseMap.get("classify_id"));
            jedis.zrem(MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_PLATFORM_COURSE_CLASSIFY_PREDICTION, map), courseId);
            if(jedis.zrank(MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_PLATFORM_COURSE_CLASSIFY_FINISH, map),courseId)==null){
                jedis.zadd(MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_PLATFORM_COURSE_CLASSIFY_FINISH, map),  MiscUtils.convertInfoToPostion( now.getTime(),position), courseId);//在结束中增加
            }

            //1.5如果课程标记为结束，则清除该课程的禁言缓存数据
            map.put(Constants.CACHED_KEY_COURSE_FIELD, courseMap.get("lecturer_id"));
            String banKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_BAN_USER_LIST, map);
            jedis.del(banKey);

            //1.6更新课程缓存信息
            Map<String, String> updateCacheMap = new HashMap<String, String>();
            updateCacheMap.put("update_time", MiscUtils.convertObjectToLong(course.get("end_time")) + "");
            updateCacheMap.put("end_time", MiscUtils.convertObjectToLong(course.get("end_time")) + "");
            updateCacheMap.put("status", "2");
            jedis.hmset(courseKey, updateCacheMap);

            ////发送结束推送消息
            SimpleDateFormat sdf =   new SimpleDateFormat("yyyy年MM月dd日HH:mm");
            String str = sdf.format(now);
            String courseEndMessage = "直播结束于"+str;
            long currentTime = System.currentTimeMillis();
            String mGroupId = jedis.hget(courseKey,"im_course_id");

            String sender = "system";
            String message = courseEndMessage;
            Map<String,Object> infomation = new HashMap<>();
            infomation.put("course_id", courseId);
            infomation.put("creator_id", courseMap.get("lecturer_id"));
            infomation.put("message", message);
            infomation.put("message_id",MiscUtils.getUUId());
            infomation.put("message_imid",infomation.get("message_id"));
            infomation.put("message_type", "1");
            infomation.put("send_type", "6");
            infomation.put("create_time", currentTime);
            if(type.equals("2")){
                //课程直播超时结束
                //1.9极光推送结束消息
                JSONObject obj = new JSONObject();
                obj.put("body",String.format(MiscUtils.getConfigByKey("jpush_course_live_overtime_force_end"),MiscUtils.RecoveryEmoji(courseMap.get("course_title"))));
                obj.put("to",courseMap.get("lecturer_id"));
                obj.put("msg_type","5");
                Map<String,String> extrasMap = new HashMap<>();
                extrasMap.put("msg_type","5");
                extrasMap.put("course_id",courseId);
                extrasMap.put("im_course_id",courseMap.get("im_course_id"));
                obj.put("extras_map", extrasMap);
                JPushHelper.push(obj);
                infomation.put("is_force",1);
                infomation.put("tip",MiscUtils.getConfigByKey("over_time_message"));
            }
            Map<String,Object> messageMap = new HashMap<>();
            messageMap.put("msg_type","1");
            messageMap.put("send_time",currentTime);
            messageMap.put("information",infomation);
            messageMap.put("mid",infomation.get("message_id"));
            String content = JSON.toJSONString(messageMap);
            IMMsgUtil.sendMessageInIM(mGroupId, content, "", sender);

        }
    }


    public CoursesMapper getCoursesMapper() {
        return coursesMapper;
    }

    public void setCoursesMapper(CoursesMapper coursesMapper) {
        this.coursesMapper = coursesMapper;
    }

    /**
     * 进行异步 推送模板消息跟直播间关注者
     * @param requestEntity 参数对象
     * @param jedisUtils 缓存对象
     * @param context
     * @姜
     */
    @FunctionName("noticeCourseToFollower")
    public void noticeCourseToFollower(RequestEntity requestEntity, JedisUtils jedisUtils, ApplicationContext context) {
        Map<String, Object> reqMap = (Map<String, Object>) requestEntity.getParam();//转换参数

        log.debug("---------------将课程创建的信息推送给WX关注直播间的粉丝"+reqMap);

        Map<String, TemplateData> templateMap = (Map<String, TemplateData>) reqMap.get("templateParam");//模板消息
        Map<String, String> courseInfo = (Map<String, String>) reqMap.get("course");

        String url = MiscUtils.getConfigByKey("course_share_url_pre_fix")+ courseInfo.get("course_id");//推送url
        List<Map<String,Object>> followers = (List<Map<String, Object>>) reqMap.get("followers");//获取推送列表
        String type = (String) reqMap.get("pushType");//类型
        String templateId = null;
        Boolean isUpdateCourse = false;
        if ("1".equals(type)) {//发布课程
            templateId = MiscUtils.getConfigByKey("wpush_start_course");//创建课程的模板id
        } else if ("2".equals(type)) {//更新课程的时间
            templateId = MiscUtils.getConfigByKey("wpush_update_course");//更新课程的模板id
            isUpdateCourse = true;
        }else if("3".equals(type)){
            templateId = MiscUtils.getConfigByKey("wpush_start_series_course");//系列更新课程的模板id
        }
        Jedis jedis = jedisUtils.getJedis();
        List<String> studentIds = new ArrayList<>();

        //微信模板消息推送
        for (Map<String,Object> user: followers) {//循环推送
            String openId = user.get("web_openid").toString();
            if(!MiscUtils.isEmpty(openId)){//推送微信模板消息给微信用户
                WeiXinUtil.send_template_message(openId, templateId, url, templateMap, jedis);//推送消息
            }
            studentIds.add(user.get("user_id").toString());
        }

        //极光消息推送
        if(MiscUtils.isEmpty(studentIds)){
            return;
        }
        JSONObject obj = new JSONObject();
        Map<String,String> extrasMap = new HashMap<>();

        if ("1".equals(type)) {
            obj.put("body",String.format(MiscUtils.getConfigByKey("jpush_room_follow_new_course"), MiscUtils.RecoveryEmoji(courseInfo.get("room_name")),  MiscUtils.RecoveryEmoji(courseInfo.get("course_title"))));
            obj.put("msg_type","11");//发布新课程
            extrasMap.put("msg_type","11");
        }else if("3".equals(type)){
            obj.put("body",String.format(MiscUtils.getConfigByKey("jpush_series_follow_new_course"), MiscUtils.RecoveryEmoji(courseInfo.get("series_title")),  MiscUtils.RecoveryEmoji(courseInfo.get("course_title"))));
            obj.put("msg_type","11");//发布新课程
            extrasMap.put("msg_type","11");
        } else if("2".equals(type)){
            String start_time = reqMap.get("start_time").toString();
            obj.put("body",String.format(MiscUtils.getConfigByKey("jpush_course_start_time_modify"), MiscUtils.RecoveryEmoji(courseInfo.get("course_title")), start_time));
            obj.put("msg_type","13");
            extrasMap.put("msg_type","13");
        }
        obj.put("user_ids",studentIds);

        extrasMap.put("course_id",  courseInfo.get("course_id"));
        extrasMap.put("im_course_id", courseInfo.get("im_course_id"));
        obj.put("extras_map", extrasMap);
        JPushHelper.push(obj);
    }




    /**
     * 把课程创建的模板消息推送给服务号粉丝
     */
    @FunctionName("noticeCourseToServiceNoFollow")
    public void noticeCourseToServiceNoFollow(RequestEntity requestEntity, JedisUtils jedisUtils, ApplicationContext context) {
        Map<String, Object> reqMap = (Map<String, Object>) requestEntity.getParam();//转换参数
        log.debug("---------------将课程创建的信息推送给服务号的粉丝"+reqMap);

        String accessToken = reqMap.get ("accessToken").toString();
        String type = reqMap.get("pushType").toString();//类型 1 是创建课程 2 是更新课程时间
        String authorizer_appid = reqMap.get("authorizer_appid").toString();
        String courseId = reqMap.get("course_id").toString();//课程id
        String templateId = reqMap.get("template_id").toString();
        if(templateId != null){
            log.info("===================================微信发送模板消息:"+templateId+"========================================");
            Map<String, TemplateData> templateMap = (Map<String, TemplateData>) reqMap.get("templateParam");//模板数据

            String url = MiscUtils.getConfigByKey("course_share_url_pre_fix")+courseId;//推送url

            String next_openid = toServiceNoFollow(null, accessToken, url, templateId, templateMap);
            while (next_openid != null) {
                next_openid = toServiceNoFollow(next_openid, accessToken, url, templateId, templateMap);
            }
        }
    }


    /**
     * 删除用户加入课程缓存
     */
    @FunctionName("delUserAddCourseList")
    public void delUserAddCourseList(RequestEntity requestEntity, JedisUtils jedisUtils, ApplicationContext context) {
        Map<String, Object> reqMap = (Map<String, Object>) requestEntity.getParam();//转换参数

        List<Map<String, Object>> courseAllStudentList = (List<Map<String, Object>>) reqMap.get("courseAllStudentList");
        Jedis jedis = jedisUtils.getJedis();
        Map<String,Object> query = new HashMap<String,Object>();
        for(Map<String, Object> student : courseAllStudentList){
            query.put(Constants.CACHED_KEY_USER_FIELD,student.get("user_id"));
            String coursesKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_USER_COURSES, query);
            jedis.del(coursesKey);
        }
    }


    /**
     * 把课程创建的模板消息推送给服务号粉丝的具体逻辑
     */
    public String toServiceNoFollow(String next_openid, String accessToken, String url, String templateId, Map<String, TemplateData> templateMap) {
        //step1 获取粉丝信息 可能多页
        JSONObject fansInfo = WeiXinUtil.getServiceFansList(accessToken, next_openid);
        JSONArray fansOpenIDArr = fansInfo.getJSONObject("data").getJSONArray("openid");

        //step2 循环推送模板消息
        for (Object openID: fansOpenIDArr) {
            int result = WeiXinUtil.sendTemplateMessageToServiceNoFan(accessToken, String.valueOf(openID), url, templateId, templateMap);
            if (result != 0) {
                //step3 出现失败的情况（服务号可能没设置这个行业和模板ID ）就中断发送
                log.error("给第三方服务号：{} 粉丝推送模板消息出现错误：{}", accessToken, result);
                return null;
            }
        }
        if (fansInfo.getIntValue("count") == 10000) {
            return fansInfo.getString("next_openid");
        }
        return null;
    }


    /**
     * 微信推送
     * @param findFollowUserIds
     * @param templateId
     * @param templateMap
     */
    @FunctionName("weiPush")
    public void weiPush(List<String> findFollowUserIds,String templateId,String url,Map<String, TemplateData> templateMap,Jedis jedis){
        // 推送   关注的直播间有创建新的课程
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("list", findFollowUserIds);
        List<String> findOpenIds = loginInfoMapper.findLoginInfoByUserIds(map);
        if (findOpenIds!=null && findOpenIds.size()>0) {
            for (String openId : findOpenIds) {
                //TODO
                WeiXinUtil.send_template_message(openId, templateId,url, templateMap, jedis);
            }
        }
    }


}

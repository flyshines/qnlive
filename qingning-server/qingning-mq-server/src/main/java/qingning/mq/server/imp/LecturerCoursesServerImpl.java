package qingning.mq.server.imp;

import com.alibaba.fastjson.JSON;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import qingning.common.entity.RequestEntity;
import qingning.common.util.Constants;
import qingning.common.util.JedisUtils;
import qingning.common.util.MiscUtils;
import qingning.mq.persistence.mybatis.CourseAudioMapper;
import qingning.mq.persistence.mybatis.CourseImageMapper;
import qingning.mq.persistence.mybatis.CoursesMapper;
import qingning.mq.persistence.mybatis.LoginInfoMapper;
import qingning.server.AbstractMsgService;
import qingning.server.JedisBatchCallback;
import qingning.server.JedisBatchOperation;
import qingning.server.rabbitmq.MessageServer;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;

import java.util.*;

/**
 * Created by loovee on 2016/12/16.
 */
public class LecturerCoursesServerImpl extends AbstractMsgService {

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

        //1.找出系统中的所有讲师，得到讲师用户id列表
        List<String> lecturerIdList = loginInfoMapper.findRoleUserIds(Constants.USER_ROLE_LECTURER);
        Date endDate = MiscUtils.getEndTimeOfToday();
        //2.遍历讲师用户id列表，得到讲师相关课程信息列表
        for(String lecturerId : lecturerIdList){

            //2.3删除缓存中的旧的课程列表及课程信息实体
            Map<String,Object> map = new HashMap<>();
            map.put(Constants.CACHED_KEY_LECTURER_FIELD, lecturerId);
            String predictionListKey =  MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_PREDICTION, map);
            String finishListKey =  MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_FINISH, map);

            Set<String> predictionCourseIdList = jedis.zrange(predictionListKey, 0 , -1);
            Set<String> finishCourseIdList = jedis.zrange(finishListKey, 0 , -1);

            Map<String,Object> courseCacheMap = new HashMap<>();
            JedisBatchCallback callBack = (JedisBatchCallback)jedisUtils.getJedis();
            callBack.invoke(new JedisBatchOperation(){
                @Override
                public void batchOperation(Pipeline pipeline, Jedis jedis) {
                    //2.1 从数据库查询出预告列表
                    Map<String,Object> queryMap = new HashMap<>();
                    queryMap.put("lecturerId", lecturerId);
                    queryMap.put("status", "1");//已经发布（预告中）
                    List<Map<String,Object>> lecturerCoursePredictionList = coursesMapper.findLecturerCourseList(queryMap);

                    //2.2 如果预告列表不存在或者数量不出，则从数据库查询出结束列表
                    List<Map<String,Object>>  lecturerCourseFinishList = null;
                    if(MiscUtils.isEmpty(lecturerCoursePredictionList) || lecturerCoursePredictionList.size() < Constants.LECTURER_PREDICTION_COURSE_LIST_SIZE){
                        queryMap.clear();
                        queryMap.put("lecturerId", lecturerId);
                        queryMap.put("status", "2");//已经结束
                        int pageCount = Constants.LECTURER_PREDICTION_COURSE_LIST_SIZE;
                        if(! MiscUtils.isEmpty(lecturerCoursePredictionList)){
                            pageCount = pageCount - lecturerCoursePredictionList.size();
                        }
                        queryMap.put("pageCount",pageCount);
                        lecturerCourseFinishList = coursesMapper.findLecturerCourseList(queryMap);
                    }


                    //删除课程实体、PPT实体、讲课音频实体
                    if(! MiscUtils.isEmpty(predictionCourseIdList)){
                        for(String courseId : predictionCourseIdList){
                            courseCacheMap.put(Constants.CACHED_KEY_COURSE_FIELD, courseId);
                            String courseKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE, courseCacheMap);
                            pipeline.del(courseKey);

                            String pptsKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_PPTS, courseCacheMap);
                            String audiosKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_AUDIOS_JSON_STRING, courseCacheMap);
                            pipeline.del(pptsKey);
                            pipeline.del(audiosKey);
                        }
                    }

                    if(! MiscUtils.isEmpty(finishCourseIdList)){
                        for(String courseId : finishCourseIdList){
                            courseCacheMap.put(Constants.CACHED_KEY_COURSE_FIELD, courseId);
                            String courseKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE, courseCacheMap);
                            pipeline.del(courseKey);

                            String pptsKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_PPTS, courseCacheMap);
                            String audiosKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_AUDIOS_JSON_STRING, courseCacheMap);
                            pipeline.del(pptsKey);
                            pipeline.del(audiosKey);
                        }
                    }

                    pipeline.del(predictionListKey);
                    pipeline.del(finishListKey);

                    Map<String,Object> timerMap = new HashMap<>();
                    //2.4将新的课程列表及新的课程实体、PPT信息、讲课音频放入缓存中
                    if(! MiscUtils.isEmpty(lecturerCoursePredictionList)){
                        for(Map<String,Object> courseMap : lecturerCoursePredictionList){
                            Date courseStartTime = (Date)courseMap.get("start_time");
                            pipeline.zadd(predictionListKey, (double) courseStartTime.getTime(), courseMap.get("course_id").toString());
                            courseCacheMap.put(Constants.CACHED_KEY_COURSE_FIELD, courseMap.get("course_id").toString());
                            String courseKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE, courseCacheMap);
                            Map<String,String> courseStringMap = new HashMap<>();
                            MiscUtils.converObjectMapToStringMap(courseMap, courseStringMap);
                            pipeline.hmset(courseKey,courseStringMap);

                            //PPT信息
                            String pptsKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_PPTS, courseCacheMap);
                            List<Map<String,Object>> pptList = courseImageMapper.findPPTListByCourseId(courseMap.get("course_id").toString());
                            if(! MiscUtils.isEmpty(pptList)){
                                pipeline.set(pptsKey, JSON.toJSONString(pptList));
                            }

                            //讲课音频信息
                            String audiosKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_AUDIOS_JSON_STRING, courseCacheMap);
                            List<Map<String,Object>> audioList = courseAudioMapper.findAudioListByCourseId(courseMap.get("course_id").toString());
                            if(! MiscUtils.isEmpty(audioList)){
                                pipeline.set(audiosKey, JSON.toJSONString(audioList));
                            }

                            //如果课程时间为今天，则需要将其加入定时任务
                            if(courseStartTime.getTime() < endDate.getTime()){
                                timerMap.put("course_id", courseMap.get("course_id").toString());
                                timerMap.put("start_time", ((Date)courseMap.get("start_time")).getTime());
                                RequestEntity requestEntity = generateRequestEntity("MessagePushServer", Constants.MQ_METHOD_ASYNCHRONIZED, "processCourseNotStart", timerMap);
                                messagePushServerimpl.processCourseNotStart(requestEntity, jedisUtils, context);
                            }
                        }
                    }

                    if(! MiscUtils.isEmpty(lecturerCourseFinishList)){
                        for(Map<String,Object> courseMap : lecturerCourseFinishList){
                            Date courseEndTime = (Date)courseMap.get("end_time");
                            pipeline.zadd(finishListKey, (double) courseEndTime.getTime(), courseMap.get("course_id").toString());
                            courseCacheMap.put(Constants.CACHED_KEY_COURSE_FIELD, courseMap.get("course_id").toString());
                            String courseKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE, courseCacheMap);
                            Map<String,String> courseStringMap = new HashMap<>();
                            MiscUtils.converObjectMapToStringMap(courseMap, courseStringMap);
                            pipeline.hmset(courseKey,courseStringMap);

                            //PPT信息
                            String pptsKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_PPTS, courseCacheMap);
                            List<Map<String,Object>> pptList = courseImageMapper.findPPTListByCourseId(courseMap.get("course_id").toString());
                            if(! MiscUtils.isEmpty(pptList)){
                                pipeline.set(pptsKey, JSON.toJSONString(pptList));
                            }

                            //讲课音频信息
                            String audiosKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_AUDIOS_JSON_STRING, courseCacheMap);
                            List<Map<String,Object>> audioList = courseAudioMapper.findAudioListByCourseId(courseMap.get("course_id").toString());
                            if(! MiscUtils.isEmpty(audioList)){
                                pipeline.set(audiosKey, JSON.toJSONString(audioList));
                            }
                        }
                    }

                    pipeline.sync();
                }
            });
        }
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

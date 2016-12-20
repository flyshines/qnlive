package qingning.mq.server.imp;

import com.alibaba.fastjson.JSON;
import qingning.common.entity.RequestEntity;
import qingning.common.util.Constants;
import qingning.common.util.MiscUtils;
import qingning.mq.persistence.mybatis.CourseAudioMapper;
import qingning.mq.persistence.mybatis.CourseImageMapper;
import qingning.mq.persistence.mybatis.CoursesMapper;
import qingning.server.JedisBatchCallback;
import qingning.server.JedisBatchOperation;
import qingning.server.rabbitmq.MessageServer;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;

import java.util.*;

/**
 * Created by loovee on 2016/12/16.
 */
public class PlatformCoursesServerImpl extends MessageServer {

    private CoursesMapper coursesMapper;
    private CourseAudioMapper courseAudioMapper;
    private CourseImageMapper courseImageMapper;

    @Override
    public void process(RequestEntity requestEntity) throws Exception {
        //将讲师的课程列表放入缓存中
        processPlatformCoursesCache();
    }


    private void processPlatformCoursesCache() {
        Jedis jedis = jedisUtils.getJedis();

        String predictionListKey =  Constants.CACHED_KEY_PLATFORM_COURSE_PREDICTION;
        String finishListKey =  Constants.CACHED_KEY_PLATFORM_COURSE_FINISH;
        Set<String> predictionCourseIdList = jedis.zrange(predictionListKey, 0, -1);
        Set<String> finishCourseIdList = jedis.zrange(finishListKey, 0, -1);

        Map<String,Object> courseCacheMap = new HashMap<>();
        JedisBatchCallback callBack = (JedisBatchCallback)jedisUtils.getJedis();
        callBack.invoke(new JedisBatchOperation(){
            @Override
            public void batchOperation(Pipeline pipeline, Jedis jedis) {

                //1.删除缓存中的旧的课程列表及课程信息实体
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

                //2.查找系统中的预告课程
                Map<String,Object> queryMap = new HashMap<>();
                queryMap.put("status", "1");//已经发布（预告中）
                List<Map<String,Object>> coursePredictionList = coursesMapper.findPlatformCourseList(queryMap);

                //3.如果预告课程数量不足，则补充查询结束课程
                List<Map<String,Object>> courseFinishList = null;
                if(MiscUtils.isEmpty(coursePredictionList) || coursePredictionList.size() < Constants.PLATFORM_PREDICTION_COURSE_LIST_SIZE){
                    int pageCount = Constants.PLATFORM_PREDICTION_COURSE_LIST_SIZE;
                    if(! MiscUtils.isEmpty(coursePredictionList)){
                        pageCount = pageCount - coursePredictionList.size();
                    }
                    queryMap.clear();
                    queryMap.put("status", "2");//已经结束
                    queryMap.put("pageCount", pageCount);
                    courseFinishList = coursesMapper.findPlatformCourseList(queryMap);
                }


                //4.将新的课程列表及新的课程实体放入缓存中
                if(! MiscUtils.isEmpty(coursePredictionList)){
                    for(Map<String,Object> courseMap : coursePredictionList){
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

                    }
                }

                if(! MiscUtils.isEmpty(courseFinishList)){
                    for(Map<String,Object> courseMap : courseFinishList){
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

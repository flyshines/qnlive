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
import redis.clients.jedis.Response;
import redis.clients.jedis.Tuple;

import java.util.*;

/**
 * Created by GHS on 2017/6/2.
 */
public class ClassIfyCourseServerImpl  extends AbstractMsgService {


    private static Logger log = LoggerFactory.getLogger(ClassIfyCourseServerImpl.class);
    private CoursesMapper coursesMapper;
    private ClassifyInfoMapper classifyInfoMapper;
    @Override
    public void process(RequestEntity requestEntity, JedisUtils jedisUtils, ApplicationContext context) throws Exception {
        processClassIfyCoursesCache(requestEntity, jedisUtils, context);
    }


    private void processClassIfyCoursesCache(RequestEntity requestEntity, JedisUtils jedisUtils, ApplicationContext context) {
        String appName = requestEntity.getAppName();
        ((JedisBatchCallback)jedisUtils.getJedis(appName)).invoke(new JedisBatchOperation(){
            private void processCached(Set<String> lecturerSet, Pipeline pipeline, Jedis jedis){
                log.debug("执行课程列表刷新工作");
                for(String lecturerId : lecturerSet) {//删除老师的
                    Map<String, Object> map = new HashMap<>();
                    map.put(Constants.CACHED_KEY_LECTURER_FIELD, lecturerId);
                    jedis.del(MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_PREDICTION, map));
                    jedis.del(MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_FINISH, map));
                }
                //删除平台
                jedis.del(Constants.CACHED_KEY_PLATFORM_COURSE_PREDICTION);
                jedis.del(Constants.CACHED_KEY_PLATFORM_COURSE_FINISH);

                String likeAppNmae = "%"+appName+"%";
                List<Map<String, Object>> classifyList = classifyInfoMapper.findClassifyInfoByAppName(likeAppNmae);
                for(Map<String, Object> classify :classifyList ){
                    String classify_id = classify.get("classify_id").toString();
                    Map<String,Object> map = new HashMap<>();
                    map.put("appName",appName);
                    map.put("classify_id",classify_id);
                    List<Map<String, Object>> courseByClassifyId = coursesMapper.findCourseByClassifyId(map);
                    for(Map<String, Object> course : courseByClassifyId){
                        if(course.get("status").equals("2") || course.get("status").equals("1")){
                            String course_id = course.get("course_id").toString();
                            String lecturer_id = course.get("lecturer_id").toString();
                            map.put(Constants.CACHED_KEY_CLASSIFY, classify_id);//?γ?id
                            map.put(Constants.CACHED_KEY_LECTURER_FIELD, lecturer_id);
                            map.put("course_id",course_id);
                            String courseClassifyIdKey = "";
                            String courseLectureKey = "";
                            String courseListKey = "";
                            Long time = 0L ;
                            if(course.get("status").equals("2")){
                                courseListKey = Constants.CACHED_KEY_PLATFORM_COURSE_FINISH;
                                courseClassifyIdKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_PLATFORM_COURSE_CLASSIFY_FINISH, map);
                                courseLectureKey =  MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_FINISH, map);
                                time = MiscUtils.convertObjectToLong(course.get("end_time"));//
                            }else{
                                courseListKey = Constants.CACHED_KEY_PLATFORM_COURSE_PREDICTION;
                                courseClassifyIdKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_PLATFORM_COURSE_CLASSIFY_PREDICTION, map);//????
                                courseLectureKey =  MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_PREDICTION, map);
                                time = MiscUtils.convertObjectToLong(course.get("start_time"));//Long.valueOf(course.get("start_time").toString());
                            }
                            String courseKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE, map);//"SYS:COURSE:{course_id}"
                            long lpos = MiscUtils.convertInfoToPostion(time, MiscUtils.convertObjectToLong(course.get("position")));
                            jedis.zadd(courseClassifyIdKey,lpos, course_id);
                            jedis.zadd(courseLectureKey,lpos, course_id);
                            jedis.zadd(courseListKey,lpos, course_id);
                            if(course.get("status").equals("2")){
                                if(jedis.hget(courseKey, "status").equals("1")){
                                    jedis.hset(courseKey,"status","2");
                                }
                            }
                        }
                    }
                }
                log.debug("执行课程列表刷新工作---------完成");
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

    public ClassifyInfoMapper getClassifyInfoMapper() {
        return classifyInfoMapper;
    }

    public void setClassifyInfoMapper(ClassifyInfoMapper classifyInfoMapper) {
        this.classifyInfoMapper = classifyInfoMapper;
    }
}
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

        ((JedisBatchCallback)jedisUtils.getJedis()).invoke(new JedisBatchOperation(){
            private void processCached(Set<String> lecturerSet, Pipeline pipeline, Jedis jedis){
                log.debug("执行课程列表刷新工作");
                for(String lecturer_id : lecturerSet){
                    Map<String,Object> map = new HashMap<>();
                    map.put("lecturer_id",lecturer_id);
                    String lecturerCoursesAllKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_ALL, map);
//                    String courseLecturePredictionKey =  MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_PREDICTION, map);
//                    String courseLectureFinishKey =  MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_FINISH, map);
                  //  String lecturerCoursesPredicionAllKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_PREDICTION_ALL, map);
                    jedis.del(lecturerCoursesAllKey);
//                    jedis.del(courseLecturePredictionKey);
//                    jedis.del(courseLectureFinishKey);
                 //   jedis.del(lecturerCoursesPredicionAllKey);
                }
                //删除平台
                jedis.del(Constants.CACHED_KEY_PLATFORM_COURSE_PREDICTION);
                jedis.del(Constants.CACHED_KEY_PLATFORM_COURSE_FINISH);
                jedis.del(Constants.SYS_COURSES_RECOMMEND_PREDICTION);
                jedis.del(Constants.SYS_COURSES_RECOMMEND_FINISH);
                jedis.del(Constants.SYS_COURSES_RECOMMEND_LIVE);
                List<Map<String, Object>> classifyList = classifyInfoMapper.findClassifyInfoBy();
                for(Map<String, Object> classify :classifyList ){
                    String classify_id = classify.get("classify_id").toString();
                    Map<String,Object> map = new HashMap<>();

                    map.put("classify_id",classify_id);
                    String courseClassifyIdFinishKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_PLATFORM_COURSE_CLASSIFY_FINISH, map);
                    String courseClassifyIdPredictionKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_PLATFORM_COURSE_CLASSIFY_PREDICTION, map);//????
                    jedis.del(courseClassifyIdFinishKey);
                    jedis.del(courseClassifyIdPredictionKey);

                    List<Map<String, Object>> courseByClassifyId = coursesMapper.findCourseByClassifyId(map);
                    for(Map<String, Object> course : courseByClassifyId){
                        String lecturerCoursesAllKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_ALL, course);
                       // String lecturerCoursesPredicionAllKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_PREDICTION_ALL, map);
                        long lpos = MiscUtils.convertInfoToPostion(MiscUtils.convertObjectToLong(course.get("start_time")) , MiscUtils.convertObjectToLong(course.get("position")));
                        jedis.zadd(lecturerCoursesAllKey,lpos,course.get("course_id").toString());
                        if( course.get("course_updown").toString().equals("1")){
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
                                    // courseLectureKey =  MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_FINISH, map);
                                    time = MiscUtils.convertObjectToLong(course.get("end_time"));//
                                }else{
                                    courseListKey = Constants.CACHED_KEY_PLATFORM_COURSE_PREDICTION;
                                    courseClassifyIdKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_PLATFORM_COURSE_CLASSIFY_PREDICTION, map);//????
                                  //  courseLectureKey =  MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_PREDICTION, map);
                                    time = MiscUtils.convertObjectToLong(course.get("start_time"));//Long.valueOf(course.get("start_time").toString());
                                }
                                String courseKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE, map);//"SYS:COURSE:{course_id}"
                                lpos = MiscUtils.convertInfoToPostion(time, MiscUtils.convertObjectToLong(course.get("position")));
                                jedis.zadd(courseClassifyIdKey,lpos, course_id);
                            //    jedis.zadd(courseLectureKey,lpos, course_id);
                                jedis.zadd(courseListKey,lpos, course_id);
                                if(course.get("status").equals("2")){
                                    if(jedis.hget(courseKey, "status").equals("1")){
                                        jedis.hset(courseKey,"status","2");
                                    }
                                }
                            }
                            //统计热门推荐
                            Long student_num = Long.valueOf(course.get("student_num").toString());
                            Long extra_num = Long.valueOf(course.get("extra_num").toString());
                            Long time = MiscUtils.convertObjectToLong(course.get("start_time"));
                            MiscUtils.courseTranferState(System.currentTimeMillis(), course,time);
                            Long lops = student_num + extra_num;
                            String course_id = course.get("course_id").toString();
                            if(!course.get("course_updown").toString().equals("0")){
                                switch (course.get("status").toString()){
                                    case "1":
                                        jedis.zadd(Constants.SYS_COURSES_RECOMMEND_PREDICTION,lops,course_id);
                                        break;
                                    case "2":
                                        jedis.zadd(Constants.SYS_COURSES_RECOMMEND_FINISH,lops,course_id);
                                        break;
                                    case "4":
                                        jedis.zadd(Constants.SYS_COURSES_RECOMMEND_LIVE,lops,course_id);
                                        break;
                                }
                            }

                        }
                    }
                }
                log.debug("执行课程列表刷新工作---------完成");
                for(String lecturerId : lecturerSet) {
                    //删除缓存中的旧的课程列表及课程信息实体
                    Map<String, Object> map = new HashMap<>();
                    map.put(Constants.CACHED_KEY_LECTURER_FIELD, lecturerId);
                    String predictionListKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_PREDICTION, map);
                    String finishListKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_FINISH, map);
                    jedis.del(predictionListKey);
                    jedis.del(finishListKey);
                    List<Map<String, Object>> lecturerCourseList = coursesMapper.findLecturerCourseList(map);
                    for(Map<String, Object> course : lecturerCourseList){
                        Long time = 0L ;
                        if( course.get("course_updown").toString().equals("1")){
                            if(course.get("status").toString().equals("2")){
                                time = MiscUtils.convertObjectToLong(course.get("end_time"));//Long.valueOf(course.get("end_time").toString());
                                long lpos = MiscUtils.convertInfoToPostion(time, MiscUtils.convertObjectToLong(course.get("position")));
                                jedis.zadd(finishListKey, lpos,course.get("course_id").toString());//在结束中增加
                            }else if(course.get("status").toString().equals("1")){
                                time = MiscUtils.convertObjectToLong(course.get("end_time"));//Long.valueOf(course.get("end_time").toString());
                                long lpos = MiscUtils.convertInfoToPostion(time, MiscUtils.convertObjectToLong(course.get("position")));
                                jedis.zadd(predictionListKey, lpos,course.get("course_id").toString());//在结束中增加
                            }
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

    public ClassifyInfoMapper getClassifyInfoMapper() {
        return classifyInfoMapper;
    }

    public void setClassifyInfoMapper(ClassifyInfoMapper classifyInfoMapper) {
        this.classifyInfoMapper = classifyInfoMapper;
    }
}

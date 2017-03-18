package qingning.mq.server.imp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import qingning.common.entity.RequestEntity;
import qingning.common.util.Constants;
import qingning.common.util.JedisUtils;
import qingning.common.util.MiscUtils;
import qingning.db.common.mybatis.persistence.CoursesMapper;
import qingning.db.common.mybatis.persistence.CoursesStudentsMapper;
import qingning.db.common.mybatis.persistence.LecturerMapper;
import qingning.server.AbstractMsgService;
import qingning.server.annotation.FunctionName;
import redis.clients.jedis.Jedis;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by Administrator on 2017/3/16.
 */
public class CourseRobotSevice extends AbstractMsgService {

    private static Logger log = LoggerFactory.getLogger(ImMsgServiceImp.class);

    @Autowired
    private LecturerMapper lecturerMapper;

    @Autowired
    private CoursesStudentsMapper coursesStudentsMapper;

    @Autowired
    private CoursesMapper coursesMapper;
    //coursesStudentsMapper.findCourseRecommendUserNum(reqMap)
    //Integer insertCount = coursesStudentsMapper.insertStudent(student);
    //coursesMapper.increaseStudent(course_id);

    protected static ConcurrentLinkedQueue<Map<String, String>> notJoinRobots                               = new ConcurrentLinkedQueue<>();

    protected static ConcurrentHashMap<String, ConcurrentLinkedQueue<Map<String, String>>> joinRobots       = new ConcurrentHashMap<>();

    protected static boolean                                                                didInitRobots   = false;

    private void robotInit() {
        synchronized (this) {
            if (didInitRobots) return;
            List<Map<String, String>> robotList = lecturerMapper.findRobotUsers("robot");// 机器人
            if (robotList != null) {
                notJoinRobots.addAll (robotList);
                didInitRobots = true;
            }
        }
    }

    //机器人管理
    private void robotManage(String course_id, Jedis jedis) {
        final String key = "robot_in_" + course_id;

        //该课程的机器人管理线程已经开启
        Map<String,Object> query = new HashMap<String,Object>();
        query.put(Constants.CACHED_KEY_COURSE_ROBOT_FIELD, course_id);
        String course_robot_key = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_ROBOT, query);
        String existRobotManage = jedis.get(course_robot_key);
        if (existRobotManage != null) {
            return;
        }
        jedis.set("course_robot_key", "1");

        //机器人数量 少于100
        if (notJoinRobots.size() < 10) {
            return;
        }

        //有机器人参与的课程总量 大于50
        if (joinRobots.size() > 50) {
            return;
        }

        //课程是讲师公开课 或者 青柠公开课

        //

        // 开启线程管理直播间的机器人
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean robotEnough = false;
                boolean courseEnd = false;
                Map<String, String> map = new HashMap<>();

                while (!robotEnough && !courseEnd) {
                    log.debug ("=====创建直播,直播机器人准备进入了直播间====");
                    try {
                        //a、1,课程预告开始——课程结束，00：30——8：00每小时增量N：0-2，每60分钟分N次随机；
                        //   2,08：01——19：00每小时增量N：2-5，每60分钟分N次随机；
                        //   3,19：01——00：30每小时增量N：5-10，每60分钟分N次随机；
                        //b、每加入一个真实听众，同时增加1—2个假听众；
                        map.clear();
                        map.put(Constants.CACHED_KEY_COURSE_FIELD, course_id);
                        String courseKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE, map);
                        Map<String,String> courseInfoMap = jedis.hgetAll(courseKey);
                        //课程结束
                        if (courseInfoMap == null ||  courseInfoMap.get("end_time") != null) {
                            courseEnd = true;
                            continue;
                        }

                        long start_time = Long.parseLong(courseInfoMap.get("start_time"));
                        long currentTimeS = System.currentTimeMillis();
                        long delta = currentTimeS - start_time;

                        long sleepTime = 60*60*1000;
                        int robotNum = 0;
                        if (delta > 24*60*60*1000) { //大于24小时
                            continue;
                        } else if (delta > 16*60*60*1000) { //16-24
                            robotNum = (int) (0 + Math.random () * 2);
                        } else if (delta > 8*60*60*1000) { //8-16
                            robotNum = (int) (2 + Math.random () * 3);
                        } else if (delta > 0) { //0-8
                            robotNum = (int) (5 + Math.random () * 5);
                        } else { //课程已经开始
                            sleepTime = 60*1000;

                            robotNum = (int) (1 + Math.random () * 1);
                        }

                        Thread.sleep (sleepTime);

                        for ( int i = 0 ; i < robotNum; i++ ) {
                            Map<String, String> user = notJoinRobots.poll ();
                            if (user != null) {
                                log.debug ("=====创建直播,直播机器人:" + user.get("nick_name") + "进入了课程====" + courseInfoMap.get("course_id"));
                                robotJoinCourse(jedis, courseInfoMap, user);

                                if (joinRobots.get (key) != null) {
                                    joinRobots.get (key).offer (user);
                                } else {
                                    ConcurrentLinkedQueue<Map<String, String>> joinRobotQueue = new ConcurrentLinkedQueue<> ();
                                    joinRobotQueue.offer (user);
                                    joinRobots.put (key, joinRobotQueue);
                                }
                            }
                            int second = (int) (10 + Math.random () * 20);
                            Thread.sleep (second * 1000);
                        }

                        //c、最多当真实用户达到30—40选个随机数，停止加机器人；
                        //d、机器人加到50—120，选个随机数之后不再增加；
                        int real_student_num = Integer.parseInt(courseInfoMap.get("real_student_num"));
                        int robot_num = joinRobots.get (key).size();
                        if (robot_num > 50 || (real_student_num - robot_num) > 30) {
                            robotEnough = true;
                        }


                    } catch (InterruptedException e) {}
                }

                robotLeaveCourse(jedis, course_id);

            }
        }).start();
    }

    //b、每加入一个真实听众，同时增加1—2个假听众；
    private void robotInByStudent (String course_id, Jedis jedis) {
        final String key = "robot_in_" + course_id;

        Map<String, String> map = new HashMap<>();
        map.put(Constants.CACHED_KEY_COURSE_FIELD, course_id);
        String courseKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE, map);
        Map<String,String> courseInfoMap = jedis.hgetAll(courseKey);
        //课程是讲师公开课 或者 青柠公开课

        //课程结束
        if (courseInfoMap == null ||  courseInfoMap.get("end_time") != null) {
            return;
        }

        //c、最多当真实用户达到30—40选个随机数，停止加机器人；
        //d、机器人加到50—120，选个随机数之后不再增加；
        int real_student_num = Integer.parseInt(courseInfoMap.get("real_student_num"));
        int robot_num = joinRobots.get (key).size();
        if (robot_num > 50 || (real_student_num - robot_num) > 30) {
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    int robotNum = (int) (1 + Math.random () * 1);
                    for (int i = 0 ; i < robotNum; i++ ) {
                        Map<String, String> user = notJoinRobots.poll ();
                        if (user != null) {
                            log.debug ("=====创建直播,直播机器人:" + user.get("nick_name") + "进入了课程====" + courseInfoMap.get("course_id"));
                            robotJoinCourse(jedis, courseInfoMap, user);

                            if (joinRobots.get (key) != null) {
                                joinRobots.get (key).offer (user);
                            } else {
                                ConcurrentLinkedQueue<Map<String, String>> joinRobotQueue = new ConcurrentLinkedQueue<> ();
                                joinRobotQueue.offer (user);
                                joinRobots.put (key, joinRobotQueue);
                            }
                        }
                        int second = (int) (10 + Math.random () * 20);
                        Thread.sleep (second * 1000);
                    }
                } catch (InterruptedException e) {}
            }
        }).start();
    }

    //机器人加入课程 只允许讲师的公开课 和 青柠的公开课
    private void robotJoinCourse(Jedis jedis, Map<String, String> courseInfoMap, Map<String, String> user) {
        //1 课程消息已有
        //2 不是讲师
        //3 是公开课
        //4 是否参加过该课程
//        Map<String,Object> studentQueryMap = new HashMap<>();
//        studentQueryMap.put("user_id", user.get("user_id"));
//        studentQueryMap.put("course_id", courseInfoMap.get("course_id"));
//        if(userModuleServer.isStudentOfTheCourse(studentQueryMap)){
//            throw new QNLiveException("100004");
//        }
        String course_id = courseInfoMap.get("course_id");

        //5 学员信息到 学员参与表中
        Date now = new Date();
        Map<String,Object> student = new HashMap<String,Object>();
        student.put("student_id", MiscUtils.getUUId());
        student.put("user_id", user.get("user_id"));
        student.put("lecturer_id", courseInfoMap.get("lecturer_id"));
        student.put("room_id", courseInfoMap.get("room_id"));
        student.put("course_id", courseInfoMap.get("course_id"));
        student.put("course_password", courseInfoMap.get("course_password"));
        student.put("student_type", "0"); //TODO distribution case
        student.put("create_time", now);
        student.put("create_date", now);
        coursesStudentsMapper.insertStudent(student);

        //6 修改讲师的课程参与人数
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(Constants.CACHED_KEY_LECTURER_FIELD, courseInfoMap.get("lecturer_id"));
        String lecturerKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER, map);
        jedis.hincrBy(lecturerKey, "total_student_num", 1);
        //7 修改用户加入的课程数
        map.clear();
        map.put(Constants.CACHED_KEY_COURSE_FIELD, course_id);
        String courseKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE, map);
        Long nowStudentNum = 0L;

        if(jedis.exists(courseKey)){
            map.clear();
            map.put("course_id", course_id);
            map.put("room_id", courseInfoMap.get("room_id"));
            Map<String,Object> numInfo = coursesStudentsMapper.findCourseRecommendUserNum(map);
            long num = 0;
            if(!MiscUtils.isEmpty(numInfo)){
                num=MiscUtils.convertObjectToLong(numInfo.get("recommend_num"));
            }
            jedis.hset(courseKey, "student_num", num+"");
        }else {
            coursesMapper.increaseStudent(course_id);
        }

        //7.修改用户缓存信息中的加入课程数
//        map.clear();
//        map.put(Constants.CACHED_KEY_USER_FIELD, user.get("user_id"));
//        String userCacheKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_USER, map);
//        if(jedis.exists(userCacheKey)){
//            jedis.hincrBy(userCacheKey, "course_num", 1L);
//        } else {
//            jedis.hincrBy(userCacheKey, "course_num", 1L);
//        }
        //8 付费课程推送消息

    }

    //机器人离开课程
    private void robotLeaveCourse(Jedis jedis, String course_id) {
        final String key = "robot_in_" + course_id;
        while (joinRobots.get(key)!=null && joinRobots.get (key).size () > 0) {
            Map<String, String> user = joinRobots.get (key).poll ();
            if (user != null) {
                log.debug ("=====直播机器人开始退出=====");
                notJoinRobots.offer (user);
            }
        }
        joinRobots.remove(key);
    }

    @FunctionName("courseCreateAndRobotStart")
    public void courseCreateAndRobotStart(RequestEntity requestEntity, JedisUtils jedisUtils, ApplicationContext context) {
        Map<String, Object> reqMap = (Map<String, Object>) requestEntity.getParam();
        String course_id = (String) reqMap.get("course_id");
        if (!didInitRobots) {
            robotInit();
        }
        robotManage(course_id, jedisUtils.getJedis());
    }

    @FunctionName("courseHaveStudentIn")
    public void courseHaveStudentIn(RequestEntity requestEntity, JedisUtils jedisUtils, ApplicationContext context) {
        Map<String, Object> reqMap = (Map<String, Object>) requestEntity.getParam();
        String course_id = (String) reqMap.get("course_id");
        if (!didInitRobots) {
            robotInit();
        }
        robotInByStudent(course_id, jedisUtils.getJedis());
    }

}

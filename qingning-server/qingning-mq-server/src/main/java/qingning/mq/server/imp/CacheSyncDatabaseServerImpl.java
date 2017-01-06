package qingning.mq.server.imp;

import org.springframework.context.ApplicationContext;
import qingning.common.entity.RequestEntity;
import qingning.common.util.Constants;
import qingning.common.util.JedisUtils;
import qingning.common.util.MiscUtils;
import qingning.mq.persistence.entity.Courses;
import qingning.mq.persistence.entity.Lecturer;
import qingning.mq.persistence.entity.LiveRoom;
import qingning.mq.persistence.mybatis.*;
import qingning.server.AbstractMsgService;
import redis.clients.jedis.Jedis;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by loovee on 2016/12/20.
 */
public class CacheSyncDatabaseServerImpl extends AbstractMsgService {

    private CoursesMapper coursesMapper;
    private LiveRoomMapper liveRoomMapper;
    private LecturerMapper lecturerMapper;
    private LoginInfoMapper loginInfoMapper;

    @Override
    public void process(RequestEntity requestEntity, JedisUtils jedisUtils, ApplicationContext context) throws Exception {
        syncCourseStudentNum(requestEntity, jedisUtils, context);
        syncCourseNum(requestEntity, jedisUtils, context);
        syncLiveRoomNum(requestEntity, jedisUtils, context);
        syncFansNum(requestEntity, jedisUtils, context);
    }

    //粉丝数(冗余存)（讲师和直播间缓存）
    private void syncFansNum(RequestEntity requestEntity, JedisUtils jedisUtils, ApplicationContext context) {
        Jedis jedis = jedisUtils.getJedis();
        //查询出所有讲师
        //同步讲师数据
        List<String> lecturerIdList = loginInfoMapper.findRoleUserIds(Constants.USER_ROLE_LECTURER);
        Map<String,Object> map = new HashMap<>();
        Lecturer lecturer = new Lecturer();
        for(String lecturerId : lecturerIdList){
            map.put(Constants.CACHED_KEY_LECTURER_FIELD, lecturerId);
            String lecturerKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER, map);
            if(! MiscUtils.isEmpty(lecturerKey)){
                String fans_num = jedis.hget(lecturerKey, "fans_num");
                if(! MiscUtils.isEmpty(fans_num)){
                    lecturer.setLecturerId(lecturerId);
                    lecturer.setFansNum(Long.parseLong(fans_num));
                    lecturerMapper.updateByPrimaryKeySelective(lecturer);
                }
            }
        }

        //同步直播间数据
        //查询出缓存中讲师的所有直播间
        for(String lecturerId : lecturerIdList){
            map.put(Constants.CACHED_KEY_LECTURER_FIELD, lecturerId);
            String lecturerRoomsKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER_ROOMS, map);
            if(! MiscUtils.isEmpty(lecturerRoomsKey)){
                Set<String> liveRoomIdList = jedis.hkeys(lecturerRoomsKey);
                if(liveRoomIdList != null && liveRoomIdList.size() > 0){
                    for(String liveRoomId : liveRoomIdList){
                        map.put(Constants.CACHED_KEY_LECTURER_FIELD, lecturerId);
                        map.put(Constants.FIELD_ROOM_ID, liveRoomId);
                        String liveRoomKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_SPECIAL_LECTURER_ROOM, map);

                        if(! MiscUtils.isEmpty(liveRoomKey)){
                            String fans_num = jedis.hget(liveRoomKey, "fans_num");
                            if(! MiscUtils.isEmpty(fans_num)){
                                LiveRoom liveRoom = new LiveRoom();
                                liveRoom.setRoomId(liveRoomId);
                                liveRoom.setFansNum(Long.parseLong(fans_num));
                                liveRoomMapper.updateByPrimaryKeySelective(liveRoom);
                            }

                            //将直播间分享URL刷入缓存中
                            jedis.hset(liveRoomKey,"room_address",MiscUtils.getConfigByKey("live_room_share_url_pre_fix")+liveRoomId);
                        }
                    }
                }
            }
        }

    }

    //直播间数（讲师缓存）
    private void syncLiveRoomNum(RequestEntity requestEntity, JedisUtils jedisUtils, ApplicationContext context) {
        Jedis jedis = jedisUtils.getJedis();
        //查询出所有讲师
        //同步讲师数据
        List<String> lecturerIdList = loginInfoMapper.findRoleUserIds(Constants.USER_ROLE_LECTURER);
        Map<String,Object> map = new HashMap<>();
        Lecturer lecturer = new Lecturer();
        for(String lecturerId : lecturerIdList){
            map.put(Constants.CACHED_KEY_LECTURER_FIELD, lecturerId);
            String lecturerKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER, map);
            if(! MiscUtils.isEmpty(lecturerKey)){
                String live_room_num = jedis.hget(lecturerKey, "live_room_num");
                if(! MiscUtils.isEmpty(live_room_num)){
                    lecturer.setLecturerId(lecturerId);
                    lecturer.setLiveRoomNum(Long.parseLong(live_room_num));
                    lecturerMapper.updateByPrimaryKeySelective(lecturer);
                }
            }
        }
    }

    //同步课程数（直播间、讲师）
    private void syncCourseNum(RequestEntity requestEntity, JedisUtils jedisUtils, ApplicationContext context) {
        Jedis jedis = jedisUtils.getJedis();
        //查询出所有讲师
        //同步讲师数据
        List<String> lecturerIdList = loginInfoMapper.findRoleUserIds(Constants.USER_ROLE_LECTURER);
        Map<String,Object> map = new HashMap<>();
        Lecturer lecturer = new Lecturer();
        for(String lecturerId : lecturerIdList){
            map.put(Constants.CACHED_KEY_LECTURER_FIELD, lecturerId);
            String lecturerKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER, map);
            if(! MiscUtils.isEmpty(lecturerKey)){
                String courseNumString = jedis.hget(lecturerKey, "course_num");
                if(! MiscUtils.isEmpty(courseNumString)){
                    lecturer.setLecturerId(lecturerId);
                    lecturer.setCourseNum(Long.parseLong(courseNumString));
                    lecturerMapper.updateByPrimaryKeySelective(lecturer);
                }
            }
        }

        //同步直播间数据
        //查询出缓存中讲师的所有直播间
        for(String lecturerId : lecturerIdList){
            map.put(Constants.CACHED_KEY_LECTURER_FIELD, lecturerId);
            String lecturerRoomsKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER_ROOMS, map);
            if(! MiscUtils.isEmpty(lecturerRoomsKey)){
                Set<String> liveRoomIdList = jedis.hkeys(lecturerRoomsKey);
                if(liveRoomIdList != null && liveRoomIdList.size() > 0){
                    for(String liveRoomId : liveRoomIdList){
                        map.put(Constants.CACHED_KEY_LECTURER_FIELD, lecturerId);
                        map.put(Constants.FIELD_ROOM_ID, liveRoomId);
                        String liveRoomKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_SPECIAL_LECTURER_ROOM, map);

                        if(! MiscUtils.isEmpty(liveRoomKey)){
                            String course_num = jedis.hget(liveRoomKey, "course_num");
                            if(! MiscUtils.isEmpty(course_num)){
                                LiveRoom liveRoom = new LiveRoom();
                                liveRoom.setRoomId(liveRoomId);
                                liveRoom.setCourseNum(Long.parseLong(course_num));
                                liveRoomMapper.updateByPrimaryKeySelective(liveRoom);
                            }
                        }
                    }
                }
            }
        }

    }

    //同步课程参与人次(讲师、课程)
    private void syncCourseStudentNum(RequestEntity requestEntity, JedisUtils jedisUtils, ApplicationContext context) {
        Jedis jedis = jedisUtils.getJedis();
        //查询出所有讲师
        List<String> lecturerIdList = loginInfoMapper.findRoleUserIds(Constants.USER_ROLE_LECTURER);
        //将讲师缓存中的人数同步到数据库中
        Map<String,Object> map = new HashMap<>();
        Lecturer lecturer = new Lecturer();
        for(String lecturerId : lecturerIdList){
            map.put(Constants.CACHED_KEY_LECTURER_FIELD, lecturerId);
            String lecturerKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER, map);
            if(! MiscUtils.isEmpty(lecturerKey)){
                String courseNumString = jedis.hget(lecturerKey, "total_student_num");
                if(! MiscUtils.isEmpty(courseNumString)){
                    lecturer.setLecturerId(lecturerId);
                    lecturer.setTotalStudentNum(Long.parseLong(courseNumString));
                    lecturerMapper.updateByPrimaryKeySelective(lecturer);
                }
            }
        }

        //缓存中查询出该讲师的课程列表
        //将课程中的参与人数同步到数据库中
        for(String lecturerId : lecturerIdList){
            map.put(Constants.CACHED_KEY_LECTURER_FIELD, lecturerId);
            String predictionListKey =  MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_PREDICTION, map);
            String finishListKey =  MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_FINISH, map);

            Set<String> predictionCourseIdList = jedis.zrange(predictionListKey, 0 , -1);
            Set<String> finishCourseIdList = jedis.zrange(finishListKey, 0 , -1);

            if(predictionCourseIdList != null && predictionCourseIdList.size() > 0){
                for(String courseId : predictionCourseIdList){
                    map.put(Constants.CACHED_KEY_COURSE_FIELD, courseId);
                    String courseKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE, map);
                    if(! MiscUtils.isEmpty(courseKey)){
                        String studentNum = jedis.hget(courseKey, "student_num");
                        if(! MiscUtils.isEmpty(studentNum)){
                            Courses courses = new Courses();
                            courses.setCourseId(courseId);
                            courses.setStudentNum(Long.parseLong(studentNum));
                            coursesMapper.updateByPrimaryKeySelective(courses);
                        }
                    }
                }
            }

            if(finishCourseIdList != null && finishCourseIdList.size() > 0){
                for(String courseId : finishCourseIdList){
                    map.put(Constants.CACHED_KEY_COURSE_FIELD, courseId);
                    String courseKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE, map);
                    if(! MiscUtils.isEmpty(courseKey)){
                        String studentNum = jedis.hget(courseKey, "student_num");
                        if(! MiscUtils.isEmpty(studentNum)){
                            Courses courses = new Courses();
                            courses.setCourseId(courseId);
                            courses.setStudentNum(Long.parseLong(studentNum));
                            coursesMapper.updateByPrimaryKeySelective(courses);
                        }
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

    public LiveRoomMapper getLiveRoomMapper() {
        return liveRoomMapper;
    }

    public void setLiveRoomMapper(LiveRoomMapper liveRoomMapper) {
        this.liveRoomMapper = liveRoomMapper;
    }

    public LecturerMapper getLecturerMapper() {
        return lecturerMapper;
    }

    public void setLecturerMapper(LecturerMapper lecturerMapper) {
        this.lecturerMapper = lecturerMapper;
    }

    public LoginInfoMapper getLoginInfoMapper() {
        return loginInfoMapper;
    }

    public void setLoginInfoMapper(LoginInfoMapper loginInfoMapper) {
        this.loginInfoMapper = loginInfoMapper;
    }
}

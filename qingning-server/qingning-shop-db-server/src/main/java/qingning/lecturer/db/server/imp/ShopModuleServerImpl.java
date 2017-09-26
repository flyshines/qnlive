
package qingning.lecturer.db.server.imp;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import qingning.common.util.Constants;
import qingning.common.util.MiscUtils;
import qingning.db.common.mybatis.persistence.*;
import qingning.server.rpc.manager.IShopModuleServer;

import java.util.*;

public class ShopModuleServerImpl implements IShopModuleServer {

    @Autowired(required = true)
    private FansMapper fansMapper;

    @Autowired(required = true)
    private LecturerMapper lecturerMapper;

    @Autowired(required = true)
    private LiveRoomMapper liveRoomMapper;

    @Autowired(required = true)
    private UserMapper userMapper;

    @Autowired(required = true)
    private LoginInfoMapper loginInfoMapper;

    @Autowired(required = true)
    private CoursesMapper coursesMapper;

    @Autowired(required = true)
    private CourseMapper courseMapper;

    @Autowired(required = true)
    private CourseImageMapper courseImageMapper;

    @Autowired(required = true)
    private CourseAudioMapper courseAudioMapper;

    @Autowired(required = true)
    private CourseMessageMapper courseMessageMapper;

    @Autowired(required = true)
    private CoursesStudentsMapper coursesStudentsMapper;

    @Autowired(required = true)
    private LecturerDistributionInfoMapper lecturerDistributionInfoMapper;

    @Autowired(required = true)
    private SystemConfigMapper systemConfigMapper;

    @Autowired(required = true)
    private SeriesMapper seriesMapper;

    @Autowired(required = true)
    private SeriesStudentsMapper seriesStudentsMapper;

    @Autowired(required = true)
    private ShopMapper shopMapper;

    @Autowired(required = true)
    private SaaSCourseMapper saasCourseMapper;

    @Autowired(required = true)
    private CourseGuestMapper courseGuestMapper;

    /**
     * 创建直播间
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Map<String, Object> createLiveRoom(Map<String, Object> reqMap) {
        Date now = new Date();
        //1.插入直播间表
        Map<String, Object> liveRoom = new HashMap<String, Object>();
        liveRoom.put("room_id", reqMap.get("room_id"));
        liveRoom.put("", reqMap.get(""));
        liveRoom.put("user_id", reqMap.get("user_id"));
        liveRoom.put("rq_code", reqMap.get("room_id"));
        liveRoom.put("room_address", reqMap.get("room_address"));
        liveRoom.put("room_name", reqMap.get("room_name"));
        liveRoom.put("avatar_address", reqMap.get("avatar_address"));
        liveRoom.put("room_remark", reqMap.get("room_remark"));
        liveRoom.put("lecturer_id", reqMap.get("user_id"));
        liveRoom.put("create_time", now);
        liveRoom.put("update_time", now);
        liveRoomMapper.insertLiveRoom(liveRoom);

        //2.如果该用户为普通用户，则需要插入讲师表，并且修改登录信息表中的身份，
        // 同时插入t_lecturer_distribution_info讲师分销信息表(统计冗余表)
        boolean isLecturer = (Boolean) reqMap.get("isLecturer");
        if (isLecturer == false) {
            //2.1插入讲师表
            Map<String, Object> lecturer = new HashMap<String, Object>();
            lecturer.put("lecturer_id", reqMap.get("user_id"));
            lecturer.put("live_room_num", 1L);
            lecturer.put("create_time", now);
            lecturer.put("update_time", now);
            lecturerMapper.insertLecture(lecturer);

            //2.2修改登录信息表 身份
            Map<String, Object> updateMap = new HashMap<>();
            updateMap.put("user_id", reqMap.get("user_id").toString());
            updateMap.put("add_role", "," + Constants.USER_ROLE_LECTURER);
            loginInfoMapper.updateUserRole(updateMap);

            //2.3插入讲师分销信息表(统计冗余表)
            Map<String, Object> lecturerDistributionInfo = new HashMap<String, Object>();
            lecturerDistributionInfo.put("lecturer_id", reqMap.get("user_id"));
            lecturerDistributionInfo.put("create_time", now);
            lecturerDistributionInfo.put("update_time", now);
            lecturerDistributionInfoMapper.insertLecturerDistributionInfo(lecturerDistributionInfo);
        }

        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("room_id", reqMap.get("room_id"));
        return resultMap;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public int insertShop(Map<String, Object> shop) {
        //店铺插入
        if(shopMapper.insert(shop)>0){
            //讲师信息插入
            Map<String,Object> lecturerInfo = new HashMap<>();
            lecturerInfo.put("lecturer_id",shop.get("lecturer_id"));
            lecturerInfo.put("create_time",new Date());
            lecturerInfo.put("shop_id",shop.get("shop_id"));
            return(lecturerMapper.insertLecture(lecturerInfo));
        }
        return 0;
    }

    @Override
    public int updateShop(Map<String, Object> param) {
        return shopMapper.updateByPrimaryKey(param);
    }

    @Override
    public Map<String, Object> findLectureByLectureId(String lecture_id) {
        return lecturerMapper.findLectureByLectureId(lecture_id);
    }

    @Override
    public Map<String, Object> findCourseByCourseId(String courseId) {
        return coursesMapper.findCourseByCourseId(courseId);
    }

    @Override
    public List<Map<String, Object>> findCourse(Map<String, Object> record) {
        return null;
    }

    @Override
    public Map<String, Object> createCourse(Map<String, Object> reqMap) {
        reqMap.put(Constants.CACHED_KEY_COURSE_FIELD, MiscUtils.getUUId());
        if(reqMap.get("goods_type").toString().equals("0")){
            reqMap.put("status", "1");
        }
        courseMapper.insertCourse(reqMap);
        return null;
    }

    @Override
    public Map<String, Object> updateCourse(Map<String, Object> reqMap) {
        Integer updateCount = 0;
        Date now = (Date)reqMap.get("now");
        Map<String,Object> course = new HashMap<String,Object>();
        course.put("course_id", reqMap.get("course_id"));
        Object start_time = reqMap.get("live_start_time");
        if(!MiscUtils.isEmpty(start_time)){
            course.put("live_start_time", new Date(MiscUtils.convertObjectToLong(start_time)));
        }
        course.put("classify_id", reqMap.get("classify_id"));
        course.put("series_id", reqMap.get("series_id"));
        course.put("course_title", reqMap.get("course_title"));
        course.put("course_image", reqMap.get("course_image"));
        course.put("course_remark", reqMap.get("course_remark"));
        course.put("course_abstract", reqMap.get("course_abstract"));
        course.put("buy_tips", reqMap.get("buy_tips"));
        course.put("target_user", reqMap.get("target_user"));
        course.put("share_url", reqMap.get("share_url"));
        course.put("course_url", reqMap.get("course_url"));
        course.put("live_start_time", reqMap.get("live_start_time"));
        course.put("live_end_time", reqMap.get("live_end_time"));
        course.put("course_duration", reqMap.get("course_duration"));
        course.put("course_type", reqMap.get("course_type"));
        course.put("course_password", reqMap.get("course_password"));
        course.put("course_price", reqMap.get("course_price"));
        course.put("series_course_updown", reqMap.get("series_course_updown"));
        course.put("course_updown", reqMap.get("course_updown"));
        course.put("updown_time", reqMap.get("updown_time"));
        course.put("shelves_sharing", reqMap.get("shelves_sharing"));
        course.put("details", reqMap.get("details"));
        course.put("update_time", now);
        updateCount=courseMapper.updateCourse(course);

        Map<String, Object> dbResultMap = new HashMap<String, Object>();
        dbResultMap.put("update_count", updateCount);
        dbResultMap.put("update_time", now);
        return dbResultMap;
    }

    @Override
    public List<Map<String, Object>> findLiveRoomByLectureId(String lecture_id) {
        return liveRoomMapper.findLiveRoomByLectureId(lecture_id);
    }

    @Override
    public Map<String, Object> findUserInfoByUserId(String userId) {
        return userMapper.findByUserId(userId);
    }

    @Override
    public List<Map<String, Object>> findSeriesStudentByMap(Map<String, Object> param) {
        return null;
    }

    @Override
    public List<Map<String, Object>> findCourseStudentByMap(Map<String, Object> param) {
        return null;
    }

    //根据union查找登录信息
    @Override
    public Map<String, Object> getLoginInfoByLoginId(String unionID) {
        return loginInfoMapper.getLoginInfoByLoginId(unionID);
    }

    @Override
    public Map<String, Object> findLastestFinishCourse(Map<String, Object> record) {
        return coursesMapper.findLastestFinishCourse(record);
    }

    @Override
    public List<Map<String, Object>> findCourseIdByStudent(Map<String, Object> reqMap) {
        return coursesStudentsMapper.findCourseIdByStudent(reqMap);
    }

    @Override
    public List<Map<String, Object>> findShopIdByFans(Map<String, Object> reqMap) {
        return fansMapper.findShopIdByFans(reqMap);
    }

    @Override
    public Map<String, Object> findSeriesBySeriesId(String series_id) {
        return seriesMapper.findSeriesBySeriesId(series_id);
    }

    @Override
    public List<Map<String, Object>> findSeriesIdByStudent(Map<String, Object> reqMap) {
        return seriesStudentsMapper.findSeriesIdByStudent(reqMap);
    }

    @Override
    public Map<String, Object> getShopInfo(String id) {
        return shopMapper.selectByPrimaryKey(id);
    }

    /**
     * 根据系列id获得该系列的所有学员列表
     */
    @Override
    public List<Map<String, Object>> findSeriesStudentListBySeriesId(String seriesId) {
        Map<String, Object> selectMap = new HashMap<>();
        selectMap.put("series_id", seriesId);
        return seriesStudentsMapper.selectSeriesStudentsByMap(selectMap);
    }


    @Override
    public Map<String, Object> findSaasCourseByCourseId(String courseId) {
        return saasCourseMapper.selectByPrimaryKey(courseId);
    }


    /**
     * 根据条件查询课程列表
     */
    @Override
    public List<Map<String, Object>> getCourseListByMap(Map<String, Object> reqMap) {
        return coursesMapper.findCourseByMap(reqMap);

    }

    /**
     * 根据条件获取嘉宾课程列表，并关联查询出课程详情
     */
    @Override
    public List<Map<String, Object>> getGuestAndCourseInfoByMap(Map<String, Object> reqMap) {
        return courseGuestMapper.findGuestAndCourseInfoByMap(reqMap);
    }

    @Override
    public List<Map<String, Object>> findSystemConfig() {
        return systemConfigMapper.findSystemConfig();
    }
}

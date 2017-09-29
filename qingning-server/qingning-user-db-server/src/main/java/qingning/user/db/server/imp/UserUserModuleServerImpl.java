
package qingning.user.db.server.imp;


import org.springframework.beans.factory.annotation.Autowired;

import qingning.common.util.MiscUtils;
import qingning.db.common.mybatis.persistence.*;
import qingning.server.rpc.manager.IUserUserModuleServer;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserUserModuleServerImpl implements IUserUserModuleServer {

    @Autowired(required = true)
    private FansMapper fansMapper;
    @Autowired(required = true)
    private UserMapper userMapper;
    @Autowired(required = true)
    private LiveRoomMapper liveRoomMapper;
    @Autowired(required = true)
    private CoursesMapper coursesMapper;
    @Autowired(required = true)
    private CoursesStudentsMapper coursesStudentsMapper;
    @Autowired(required = true)
    private CourseImageMapper courseImageMapper;
    @Autowired(required = true)
    private CourseAudioMapper courseAudioMapper;
    @Autowired(required = true)
    private RewardConfigurationMapper rewardConfigurationMapper;
    @Autowired(required = true)
    private CourseMessageMapper courseMessageMapper;
    @Autowired(required = true)
    private LoginInfoMapper loginInfoMapper;
    @Autowired(required = true)
    private LecturerCoursesProfitMapper lecturerCoursesProfitMapper;
    @Autowired(required = true)
    private LecturerMapper lecturerMapper;
    @Autowired(required = true)
    private SystemConfigMapper systemConfigMapper;
    @Autowired(required = true)
    private UserGainsMapper userGainsMapper;
    @Autowired(required = true)
    private WithdrawCashMapper withdrawCashMapper;
    @Autowired(required = true)
    private SeriesMapper seriesMapper;
    @Autowired(required = true)
    private SeriesStudentsMapper seriesStudentsMapper;
    @Autowired(required = true)
    private ShopMapper shopMapper;
    @Autowired(required = true)
    private TradeBillMapper tradeBillMapper;
    @Autowired(required = true)
    private SaaSCourseMapper saasCourseMapper;
    @Autowired(required = true)
    private AdminUserMapper adminUserMapper;

    @Override
    public Map<String, Object> findUserInfoByUserId(String user_id) {
        return userMapper.findByUserId(user_id);
    }

    @Override
    public Map<String, Object> findCourseByCourseId(String string) {
        return coursesMapper.findCourseByCourseId(string);
    }

    @Override
    public List<Map<String, Object>> findCourse(Map<String, Object> record) {
        return null;
    }

    @Override
    public List<Map<String, Object>> findRewardConfigurationList() {
        return rewardConfigurationMapper.findRewardConfigurationList();
    }

    @Override
    public boolean isStudentOfTheCourse(Map<String, Object> studentQueryMap) {
        return !MiscUtils.isEmpty(coursesStudentsMapper.isStudentOfTheCourse(studentQueryMap));
    }

    @Override
    public Map<String, Object> findLectureByLectureId(String lecture_id) {
        return lecturerMapper.findLectureByLectureId(lecture_id);
    }

    @Override
    public List<Map<String, Object>> findCourseIdByStudent(Map<String, Object> reqMap) {
        return coursesStudentsMapper.findCourseIdByStudent(reqMap);
    }

    @Override
    public Map<String, Object> getLoginInfoByLoginId(String unionID) {
        return null;
    }

    @Override
    public List<Map<String, Object>> findShopIdByFans(Map<String, Object> reqMap) {
        return fansMapper.findShopIdByFans(reqMap);
    }

    @Override
    public List<Map<String, Object>> findSeriesStudentListBySeriesId(String seriesId) {
        return null;
    }

    @Override
    public Map<String, Object> findSeriesBySeriesId(String series_id) {
        return seriesMapper.findSeriesBySeriesId(series_id);
    }

    @Override
    public List<Map<String, Object>> findSeriesIdByStudent(Map<String, Object> reqMap) {
        return seriesStudentsMapper.findSeriesIdByStudent(reqMap);
    }

    /**
     * 获取用户加入的单品课程列表
     */
	@Override
	public List<Map<String, Object>> findCourseStudentByMap(Map<String, Object> selectCourseStudentMap) {
		return coursesStudentsMapper.selectCourseStudentByMap(selectCourseStudentMap);
	}
	/**
	 * 获取用户加入的系列列表
	 */
	@Override
	public List<Map<String, Object>> findSeriesStudentByMap(Map<String, Object> reqMap) {
		return seriesStudentsMapper.selectSeriesStudentsByMap(reqMap);
	}

    //TODO 实现缓存方法
    @Override
    public List<Map<String, Object>> findLiveRoomByLectureId(String lecture_id) {
        return null;
    }
    //TODO 实现缓存方法
    @Override
    public Map<String, Object> getShopInfo(String shopId) {
        return shopMapper.selectByPrimaryKey(shopId);
    }
    //TODO 实现缓存方法
    @Override
    public List<Map<String, Object>> getGuestAndCourseInfoByMap(Map<String, Object> reqMap) {
        return null;
    }

    @Override
    public List<Map<String, Object>> findSystemConfig() {
        return systemConfigMapper.findSystemConfig();
    }

    /**
     * 查看订单
     * @param record
     * Created by DavidGong on 2017/7/4.
     * @return
     */
    @Override
    public boolean findUserWhetherToPay(Map<String, Object> record) {
        return  !MiscUtils.isEmpty(tradeBillMapper.findUserWhetherToPay(record)) ;
    }

    /**
     * 加入课程
     * @param courseMap
     * @return
     */
    @Override
    public Map<String, Object> joinCourse(Map<String, String> courseMap) {
        Date now = new Date();
        Map<String, Object> student = new HashMap<String, Object>();
        student.put("student_id", MiscUtils.getUUId());
        student.put("user_id", courseMap.get("user_id"));
        student.put("distributer_id", courseMap.get("distributer_id"));
        student.put("lecturer_id", courseMap.get("lecturer_id"));
        student.put("shop_id", courseMap.get("shop_id"));
        student.put("course_id", courseMap.get("course_id"));
        student.put("payment_amount", courseMap.get("payment_amount"));
        student.put("course_password", courseMap.get("course_password"));
        student.put("student_type", "0"); //TODO distribution case
        student.put("create_time", now);
        student.put("create_date", now);
        //students.setPaymentAmount();//TODO
        Integer insertCount = coursesStudentsMapper.insertStudent(student);
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("insertCount", insertCount);
        return resultMap;
    }
}


package qingning.common.db.server.imp;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import qingning.db.common.mybatis.persistence.*;
import qingning.server.rpc.manager.ICommonModuleServer;

import java.util.*;

public class CommonModuleServerImpl implements ICommonModuleServer {

	@Autowired(required = true)
	private ServerFunctionMapper serverFunctionMapper;

	@Autowired(required = true)
	private LoginInfoMapper loginInfoMapper;

	@Autowired(required = true)
	private UserMapper userMapper;

	@Autowired(required = true)
	private CoursesMapper coursesMapper;

	@Autowired(required = true)
	private RewardConfigurationMapper rewardConfigurationMapper;

	@Autowired(required = true)
	private TradeBillMapper tradeBillMapper;

	@Autowired(required = true)
	private PaymentBillMapper paymentBillMapper;

	@Autowired(required = true)
	private LecturerCoursesProfitMapper lecturerCoursesProfitMapper;

	@Autowired(required = true)
	private CoursesStudentsMapper coursesStudentsMapper;

	@Autowired(required = true)
	private LiveRoomMapper liveRoomMapper;

	@Autowired(required = true)
	private LecturerMapper lecturerMapper;

	@Autowired(required = true)
	private FeedbackMapper feedbackMapper;

	@Autowired(required = true)
	private VersionMapper versionMapper;

	@Autowired(required = true)
	private CourseMessageMapper courseMessageMapper;

	@Autowired(required = true)
	private CourseImageMapper courseImageMapper;

	@Autowired(required = true)
	private CourseAudioMapper courseAudioMapper;

	@Autowired(required = true)
	private FansMapper fansMapper;

	@Autowired(required = true)
	private ClassifyInfoMapper classifyInfoMapper;

	@Autowired(required = true)
	private BannerInfoMapper bannerInfoMapper;

	@Autowired(required = true)
	private SystemConfigMapper systemConfigMapper;
	@Autowired(required = true)
	private UserGainsMapper userGainsMapper;
	@Autowired(required = true)
	private WithdrawCashMapper withdrawCashMapper;
	@Autowired(required = true)
	private AdminUserMapper adminUserMapper;
	@Autowired(required = true)
	private SeriesMapper seriesMapper;
	@Autowired(required = true)
	private ShopMapper shopMapper;
	@Autowired(required = true)
	private ShopUserMapper shopUserMapper;

	@Autowired(required = true)
	private SeriesStudentsMapper seriesStudentsMapper;

	@Autowired(required = true)
	private CourseGuestMapper courseGuestMapper;

	@Autowired(required = true)
	private LecturerDistributionInfoMapper lecturerDistributionInfoMapper;
	@Override
	public List<Map<String, Object>> getServerUrls() {
		return serverFunctionMapper.getServerUrls();
	}



	@Override
	public Map<String, Object> findUserInfoByUserId(String user_id) {
		return userMapper.findByUserId(user_id);
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
	public Map<String, Object> findVersionInfoByOS(Map<String, Object> plateform) {
		return versionMapper.findVersionInfoByOS(plateform);
	}
	@Override
	public Map<String, Object> findForceVersionInfoByOS(String force_version_key) {
		return versionMapper.findForceVersionInfoByOS(force_version_key);
	}



	@Override
	public Map<String,Object> findLectureByLectureId(String lecture_id){
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
	public List<Map<String, Object>> findRewardConfigurationList() {
		return rewardConfigurationMapper.findRewardConfigurationList();
	}

	@Override
	public List<Map<String, Object>> findShopIdByFans(Map<String, Object> reqMap) {
		return fansMapper.findShopIdByFans(reqMap);
	}


	@Override
	public List<Map<String, Object>> findSystemConfig( ) {
		return systemConfigMapper.findSystemConfig();
	}



	@Override
	public List<Map<String, Object>> findLiveRoomByLectureId(String lecture_id) {
		return null;
	}

	@Override
	public Map<String, Object> getShopInfo(String shopId) {
		return shopMapper.selectByPrimaryKey(shopId);
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
	 * 创建直播间
	 */
	@Transactional(rollbackFor=Exception.class)
	@Override
	public Map<String,Object> createLiveRoom(Map<String, Object> reqMap) {
		Date now = new Date();
		//1.插入直播间表
		Map<String,Object> liveRoom = new HashMap<String,Object>();
		liveRoom.put("room_id", reqMap.get("room_id"));
		liveRoom.put("",reqMap.get(""));
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
		boolean isLecturer = (Boolean)reqMap.get("isLecturer");
		if(isLecturer == false){
			//2.1插入讲师表
			Map<String,Object> lecturer = new HashMap<String,Object>();
			lecturer.put("lecturer_id", reqMap.get("user_id"));
			lecturer.put("live_room_num", 1L);
			lecturer.put("create_time", now);
			lecturer.put("update_time", now);
			lecturerMapper.insertLecture(lecturer);

			//2.3插入讲师分销信息表(统计冗余表)
			Map<String,Object> lecturerDistributionInfo = new HashMap<String,Object>();
			lecturerDistributionInfo.put("lecturer_id", reqMap.get("user_id"));
			lecturerDistributionInfo.put("create_time", now);
			lecturerDistributionInfo.put("update_time", now);
			lecturerDistributionInfoMapper.insertLecturerDistributionInfo(lecturerDistributionInfo);
		}

		Map<String,Object> resultMap = new HashMap<>();
		resultMap.put("room_id", reqMap.get("room_id"));
		return resultMap;
	}

	@Override
	public int updateUserById(Map<String, Object> userMap) {
		//手机号码已经绑定
		if (userMapper.existByPhone(userMap) > 0) {
			return 1;
		}
		userMapper.updateUser(userMap);
		return 0;
	}
	/**
	 * 根据map中的参数查询banner
	 */
	@Override
	public List<Map<String, Object>> findBannerInfoByMap(Map<String, Object> reqMap) {
		return bannerInfoMapper.selectBannerInfoByMap(reqMap);
	}

	/**
	 * 根据map中的参数查询banner总数量
	 */
	@Override
	public int findBannerCountByMap(Map<String, Object> reqMap) {
		return bannerInfoMapper.selectBannerCountByMap(reqMap);
	}

	//TODO 实现缓存方法
	@Override
	public List<Map<String, Object>> findSeriesStudentByMap(Map<String, Object> param) {
		return null;
	}
	//TODO 实现缓存方法
	@Override
	public List<Map<String, Object>> findCourseStudentByMap(Map<String, Object> param) {
		return null;
	}
	//TODO 实现缓存方法
	@Override
	public List<Map<String, Object>> getGuestAndCourseInfoByMap(Map<String, Object> reqMap) {
		return null;
	}
}

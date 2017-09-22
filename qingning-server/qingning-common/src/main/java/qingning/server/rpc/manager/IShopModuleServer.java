package qingning.server.rpc.manager;


import qingning.server.rpc.CourseModuleServer;
import qingning.server.rpc.SeriesModuleServer;
import qingning.server.rpc.ShopModuleServer;
import qingning.server.rpc.UserModuleServer;

import java.util.List;
import java.util.Map;

public interface IShopModuleServer extends UserModuleServer,ShopModuleServer,CourseModuleServer,SeriesModuleServer {
	Map<String,Object> createLiveRoom(Map<String, Object> reqMap);

	Map<String,Object> findLiveRoomByRoomId(String room_id);

	Map<String,Object> updateLiveRoom(Map<String, Object> reqMap);

	Map<String,Object> createCourse(Map<String, Object> reqMap);

	Map<String,Object> updateCourse(Map<String, Object> reqMap);

	List<Map<String,Object>> findCourseListForLecturer(Map<String, Object> queryMap);

	void createCoursePPTs(Map<String, Object> reqMap);

	void deletePPTByCourseId(String course_id);

	List<Map<String,Object>> findPPTListByCourseId(String course_id);

	List<Map<String,Object>> findAudioListByCourseId(String course_id);

	List<Map<String,Object>> findCourseMessageList(Map<String, Object> queryMap);

	List<Map<String,Object>> findCourseStudentList(Map<String, Object> queryMap);

	List<Map<String,Object>> findCourseAllStudentList(String course_id);

	Map<String,Object> findLoginInfoByUserId(String userId);

	List<Map<String,Object>> findBanUserListInfo(Map<String,Object> banUserIdList);

	Map<String,Object> findCourseMessageMaxPos(String course_id);
	
	List<Map<String,Object>> findCourseProfitList(Map<String, Object> queryMap);
	
	List<Map<String,Object>> findRoomDistributerInfo(Map<String,Object> paramters);
	
	List<Map<String,Object>> findRoomDistributerCourseInfo(Map<String,Object> paramters);
	
	Map<String,Object> createRoomDistributer(Map<String, String> reqMap) throws Exception;

	List<String> findLatestStudentAvatarAddList(Map<String, Object> queryMap);
	
	Map<String,Object> findDistributerInfo(Map<String,Object> paramters);
	
	List<Map<String,Object>> findRoomFanList(Map<String,Object> paramters);

	Map<String,Object> findLecturerDistributionByLectureId(String user_id);

	List<Map<String, Object>> findCourseStudentListWithLoginInfo(String course_id);

	List<Map<String,Object>> findRoomFanListWithLoginInfo(String roomId);
	
	/**
	 * 根据userid 获取 openid
	 * @param map
	 * @return
	 */
	List<String> findLoginInfoByUserIds(Map<String, Object> map);
	/**
	 * 根据unionid 获取 userinfo
	 * @param
	 * @return
	 */
	Map<String,Object> getLoginInfoByLoginId(String unionID);

	int insertLecturerDistributionLink(Map<String, Object> map);

	Map<String, Object> findAvailableRoomDistributer(Map<String, Object> record);
	
	Map<String,Object> findByDistributerId(String distributer_id);
	
	List<Map<String,Object>> findFinishCourseListForLecturer(Map<String,Object> record);
	
	List<Map<String,Object>> findDistributionRoomByLectureInfo(Map<String, Object> record);
	
	/**
	 * 获取客服信息
	 */
	Map<String,Object> findCustomerServiceBySystemConfig(Map<String, Object> reqMap);

	/**
	 * 插入服务号的信息
	 */
	int insertServiceNoInfo(Map<String, String> map);
	/**
	 * 重复授权 更新服务号的信息
	 */
	int updateServiceNoInfo(Map<String, String> map);
	/**
	 * 插入服务号的信息 讲师id
	 */
	int updateServiceNoLecturerId(Map<String, String> map);

	//根据appid查找服务号信息
	Map<String, Object> findServiceNoInfoByAppid(String authorizer_appid);

	//根据lecturerId查找服务号信息
	Map<String, Object> findServiceNoInfoByLecturerId(String lecturerId);

	int updateUser(Map<String,Object> parameters);

	void updateLoginInfo(Map<String, Object> updateMap);

    Map<String,Object> findServiceTemplateInfoByLecturerId(Map<String, String> wxPushParam);

	void insertServiceTemplateInfo(Map<String, String> paramMap);


	/**
	 * 创建系列
	 * @param reqMap
	 * @return
	 */
	Map<String,Object> createSeries(Map<String, Object> reqMap);

	/**
	 * 编辑系列
	 * @param record
	 * @return
	 */
	Map<String, Object> updateSeries(Map<String,Object> record);

	Map<String, Object> updateSeriesCourse(Map<String, Object> course);

	Map<String, Object> updateSaasSeriesCourse(Map<String, Object> course);

	Map<String, Object> increaseSeriesCourse(String series_id);

	Map<String, Object> delSeriesCourse(String series_id);

	Map<String, Object> updateUpdown(Map<String,Object> record);


	Map<String, Object> updateCourseLonely(Map<String, Object> course);

	/**
	 * 获取系列课收益明细列表
	 * @param reqMap
	 * @return
	 */
	List<Map<String, Object>> findSeriesProfitListByMap(Map<String, Object> reqMap);
	/**
	 * 获取系列课收益统计（门票总收入，总收入）
	 * @param reqMap
	 * @return List:下标为0表示门票总收入;下标为1表示总收入
	 */
	List<Map<String, Object>> findSeriesProfitStatistics(Map<String, Object> reqMap);

	Map<String, Object> courseGurest(Map<String, Object> reqMap);

	/**
	 * 获取嘉宾加入过的列表
	 * @param reqMap
	 * @return
	 */
	List<Map<String, Object>> findGuestCourses(Map<String, Object> reqMap);

	List<Map<String, Object>> findCourseGuestByCourseId(String course_id);

	Map<String,Object> findByPhone(Map<String,Object> record);


	void deleteUserAndLogin(String user_id);
	/**
	 * 根据条件获取课程嘉宾记录
	 * @param selectCourseGuestMap
	 * @return
	 */
	List<Map<String, Object>> getCourseGuestByMap(Map<String, Object> selectCourseGuestMap);

	Map<String, Object> findCourseGuestByUserAndCourse(Map<String, Object> reqMap);
}

package qingning.server.rpc.manager;


import java.util.List;
import java.util.Map;

public interface IUserModuleServer {

	Map<String,Object> userFollowRoom(Map<String, Object> reqMap) throws Exception;

	void updateLiveRoomNumForUser(Map<String, Object> reqMap);
	Map<String,Object> findUserInfoByUserId(String user_id);

	Map<String,Object> findLiveRoomByRoomId(String room_id);


	Map<String,Object> findFansByUserIdAndRoomId(Map<String, Object> reqMap);

	Map<String,Object> findCourseByCourseId(String string);

	List<Map<String,Object>> findCourseListForLecturer(Map<String, Object> queryMap);

	Map<String,Object> joinCourse(Map<String, String> courseMap);

	void increaseStudentNumByCourseId(String course_id);

	List<Map<String,Object>> findPPTListByCourseId(String course_id);

	List<Map<String,Object>> findAudioListByCourseId(String course_id);

	List<Map<String,Object>> findRewardConfigurationList();


	List<Map<String,Object>> findCourseMessageList(Map<String, Object> queryMap);

	List<Map<String,Object>> findCourseStudentList(Map<String, Object> queryMap);

	Map<String,Object> findLoginInfoByUserId(String user_id);

	Map<String,Object> findCourseMessageMaxPos(String course_id);

	List<String> findLatestStudentAvatarAddList(Map<String, Object> queryMap);
	
	List<Map<String,Object>> findFanInfoByUserId(Map<String, Object> queryMap);
	
    List<Map<String,Object>> findCourseListOfStudent(Map<String, Object> queryMap);
	
	List<Map<String,Object>> findUserConsumeRecords(Map<String, Object> queryMap);

	List<Map<String,Object>> findDistributionInfoByDistributerId(Map<String, Object> queryMap);

	Map<String,Object> findAvailableRoomDistributer(Map<String, Object> queryMap);

	boolean isStudentOfTheCourse(Map<String, Object> studentQueryMap);
	
	Map<String,Object> findLectureByLectureId(String lecture_id);
	
	List<Map<String,Object>> findFinishCourseListForLecturer(Map<String,Object> record);
	
	List<Map<String,Object>> findCourseIdByStudent(Map<String, Object> reqMap);
	
	Map<String,Object> findCourseRecommendUserNum(Map<String, Object> reqMap);

	/**
	 * 获取客服信息
	 */
	Map<String,Object> findCustomerServiceBySystemConfig(Map<String, Object> reqMap);

	List<Map<String,Object>> findRoomIdByFans(Map<String, Object> reqMap);

	/**
	 * 获取没有t_user_gains记录的user_id
	 * @param limit
	 * @return
	 */
	List<Map<String,Object>> findNotGainsUserId(int limit);
	/**
	 * 获得userId列表里用户的直播间收益
	 * @param userIdList
	 * @return
	 */
	List<Map<String, Object>> findRoomAmount(List<String> userIdList);
	/**
	 * 获得userId列表里分销员总收益
	 * @param userIdList
	 * @return
	 */
	List<Map<String, Object>> findDistributerAmount(List<String> userIdList);

	/**
	 * 获取用户提现成功总金额
	 * @param userIdList
	 * @return
	 */
	List<Map<String, Object>> findUserWithdrawSum(List<String> userIdList);

	void insertUserGains(List<Map<String ,Object>> list);

	void insertUserGainsByNewUser(Map<String ,Object> reqMap);

	void updateUserGains(Map<String ,Object> reqMap);

	Map<String ,Object> findUserGainsByUserId(String user_id);

	/**
	 * 获得符合条件的首条提现记录
	 * @param selectMap
	 * @return
	 */
	Map<String, Object> findWithdrawCashByMap(Map<String, Object> selectMap);

	/**
	 * 插入提现申请表
	 * @param record
	 * @param balance
	 * @return
	 */
	int insertWithdrawCash(Map<String, Object> record, int balance);
	/**分页查询-用户提现记录
	 * @param param
	 * @return
	 */
	List<Map<String, Object>> findWithdrawList(Map<String, Object> param);
	/**查询-提现记录数
	 * @param selectMap
	 * @return
	 */
	Map<String, Object> selectWithdrawSizeById(Map<String, Object> selectMap);

	/**完成提现记录
	 * @param withdrawId
	 * @param remark
	 * @param userId
	 * @param result
	 * @param initial_amount
	 * @return
	 */
	int updateWithdraw(String withdrawId,String remark,String userId,String result,Long initial_amount);
	/**分页查询-后台提现记录
	 * @param param
	 * @return
	 */
	Map<String, Object>  findWithdrawListAll(Map<String, Object> param);

    Map<String,Object> findSeriesBySeriesId(String course_id);

	boolean isStudentOfTheSeries(Map<String, Object> queryMap);
}

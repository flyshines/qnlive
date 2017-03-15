package qingning.server.rpc.manager;


import java.util.List;
import java.util.Map;
import java.util.Objects;

public interface ICommonModuleServer {
	public List<Map<String, Object>> getServerUrls();

	Map<String,Object> getLoginInfoByLoginIdAndLoginType(Map<String, Object> reqMap);

	Map<String,String> initializeRegisterUser(Map<String, Object> reqMap);

	Map<String,Object> findLectureByLectureId(String lecture_id);
	
	Map<String,Object> findLoginInfoByUserId(String user_id);

	Map<String,Object> findUserInfoByUserId(String user_id);

	Map<String,Object> findCourseByCourseId(String courseId);

	Map<String,Object> findRewardInfoByRewardId(String reward_id);

	void insertTradeBill(Map<String, Object> insertMap);

	void closeTradeBill(Map<String, Object> failUpdateMap);

	void insertPaymentBill(Map<String, Object> insertPayMap);

	boolean isTradebillFinish(String outTradeNo);

	void updateUserWebOpenIdByUserId(Map<String, Object> updateMap);

	Map<String,Object> findTradebillByOutTradeNo(String outTradeNo);

	Map<String,Object> handleWeixinPayResult(Map<String, Object> requestMapData) throws Exception;
	
	Map<String,Object> findByDistributerId(String distributer_id);
	
	List<Map<String,Object>> findDistributionInfoByDistributerId(Map<String,Object> parameters);
	List<Map<String,Object>> findRoomDistributerRecommendInfo(Map<String,Object> parameters);
	List<Map<String,Object>> findRoomDistributerCourseInfo(Map<String,Object> parameters);
	List<Map<String,Object>> findCourseWithRoomDistributerCourseInfo(Map<String,Object> parameters);
	List<Map<String,Object>> findRoomDistributerCourseDetailsInfo(Map<String,Object> parameters);
	
	int updateUser(Map<String,Object> parameters);

	Map<String,Object> findRoomDistributerInfoByRqCode(String rqCode);

	Map<String,Object> findLiveRoomByRoomId(String room_id);

	Map<String,Object> findRoomDistributerRecommendAllInfo(Map<String, Object> queryMap);

	void insertRoomDistributerRecommend(Map<String, Object> insertMap);

	Map<String,Object> findRoomDistributionInfoByDistributerId(Map<String, Object> queryMap);

	//void increteRecommendNumForRoomDistributer(Map<String, Object> updateMap);

	void updateAfterStudentBuyCourse(Map<String, Object> updateCourseMap);

	void insertFeedback(Map<String, Object> reqMap);

	Map<String,Object> findRewardByUserIdAndCourseId(Map<String, Object> rewardQueryMap);

	Map<String,Object> findVersionInfoByOS(String plateform);
	Map<String,Object> findAvailableRoomDistributer(Map<String,Object> record);
	Map<String,Object> findForceVersionInfoByOS(String force_version_key);

	Integer updateIMAccount(Map<String, Object> updateIMAccountMap);

	Map<String,Object> findUserDistributionInfo(Map<String, Object> queryuserDistribution);

	List<Map<String,Object>> findcourseRecommendUsers(Map<String, Object> reqMap);

	Map<String,Object> findCourseRecommendUserNum(Map<String, Object> reqMap);

	Map<String,Object> findRoomDistributerRecommendItem(Map<String, Object> queryMap);

	void updateRoomDistributerRecommend(Map<String, Object> insertMap);

	List<Map<String,Object>> findRoomRecommendUserList(Map<String, Object> reqMap);

	List<Map<String,Object>> findDistributionRoomDetailList(Map<String, Object> reqMap);
	
	Map<String,Object> findDistributionRoomDetail(Map<String, Object> reqMap);
	
	List<Map<String,Object>> findCourseIdByStudent(Map<String, Object> reqMap);
	
	Map<String,Object> findCoursesSumInfo(Map<String, Object> queryMap);
	
	List<Map<String,Object>> findRoomRecommendUserListByCode(Map<String, Object> record);

	/**
	 * 查询课程消息列表
	 */
	List<Map<String,Object>> findCourseMessageListByComm(Map<String, Object> queryMap);

	int findCourseMessageSum(Map<String,Object> queryMap);

}

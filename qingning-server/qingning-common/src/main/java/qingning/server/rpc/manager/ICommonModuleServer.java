package qingning.server.rpc.manager;


import java.util.List;
import java.util.Map;
import java.util.SortedMap;

public interface ICommonModuleServer {
	public List<Map<String, Object>> getServerUrls();

	Map<String,Object> getLoginInfoByLoginIdAndLoginType(Map<String, Object> reqMap);

	Map<String,String> initializeRegisterUser(Map<String, Object> reqMap);


	Map<String,Object> findLoginInfoByUserId(String user_id);

	Map<String,Object> findUserInfoByUserId(String user_id);

	Map<String,Object> findCourseByCourseId(String courseId);

	Map<String,Object> findRewardInfoByRewardId(String reward_id);

	void insertTradeBill(Map<String, Object> insertMap);

	void updateTradeBill(Map<String, Object> failUpdateMap);

	void insertPaymentBill(Map<String, Object> insertPayMap);

	boolean findTradebillStatus(String outTradeNo);

	void updateUserWebOpenIdByUserId(Map<String, Object> updateMap);

	Map<String,Object> findTradebillByOutTradeNo(String outTradeNo);

	Map<String,Object> handleWeixinPayResult(SortedMap<String, String> requestMapData);
	
	Map<String,Object> findByDistributerId(String distributer_id);
	
	List<Map<String,Object>> findDistributionInfoByDistributerId(Map<String,Object> parameters);
	List<Map<String,Object>> findRoomDistributerRecommendInfo(Map<String,Object> parameters);
	List<Map<String,Object>> findRoomDistributerCourseInfo(Map<String,Object> parameters);
	List<Map<String,Object>> findRoomDistributerCourseDetailsInfo(Map<String,Object> parameters);
	
	int updateUser(Map<String,Object> parameters);

	Map<String,Object> findRoomDistributerInfoByRqCode(String rqCode);

	Map<String,Object> findLiveRoomByRoomId(String room_id);

	Map<String,Object> findRoomDistributerRecommendAllInfo(Map<String, Object> queryMap);

	void insertRoomDistributerRecommend(Map<String, Object> insertMap);

	List<Map<String,Object>> findRoomDistributionInfoByDistributerId(Map<String, Object> queryMap);

	void increteRecommendNumForRoomDistributer(Map<String, Object> updateMap);

	void updateAfterPayCourse(Map<String, Object> updateCourseMap);

	void insertFeedback(Map<String, Object> reqMap);
}

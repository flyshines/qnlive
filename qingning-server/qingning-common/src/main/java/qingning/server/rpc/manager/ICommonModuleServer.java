package qingning.server.rpc.manager;


import java.util.List;
import java.util.Map;

public interface ICommonModuleServer {
	List<Map<String, Object>> getServerUrls();
	List<Map<String, Object>> getServerUrlByAppName(String appName);
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

	Map<String,Object> findTradeBillByPaymentid(String pre_pay_no);

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

	Map<String,Object> findVersionInfoByOS(Map<String, Object> plateform);
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

	Map<String,Object> findCourseMessageByComm(Map<String, Object> queryMap);

	int findCourseMessageSum(Map<String,Object> queryMap);

	void updateCourseByCourseId(Map<String,Object> queryMap);


	List<Map<String,Object>> findPPTListByCourseId(String course_id);

	List<Map<String,Object>> findAudioListByCourseId(String course_id);

	Map<String,Object> findFansByUserIdAndRoomId(Map<String, Object> reqMap);

	List<Map<String,Object>> findRewardConfigurationList();

	Map<String,Object> findByPhone(Map<String,String> record);

	List<Map<String,Object>> findRoomIdByFans(Map<String, Object> reqMap);

	List<Map<String,Object>> findClassifyInfo();
	List<Map<String,Object>> findClassifyInfoByAppName(String appName);

	List<Map<String,Object>> findCourseBySearch(Map<String, Object> reqMap);

	List<Map<String, Object>> findLiveRoomBySearch(Map<String,Object> record);

	List<Map<String, Object>> findBannerInfoAll();
	List<Map<String,Object>> findBannerInfoAllByAppName(String appName);


	List<Map<String, Object>> findCourseByClassifyId(Map<String,Object> record);
	List<Map<String, Object>> findCourseByStatus(Map<String,Object> record);

    Integer insertCourseMessageList(List<Map<String, Object>> messageList);

	public List<Map<String,Object>> findSystemConfig();

	public List<Map<String,Object>> findSystemConfigByAppName(String appName);

    void updateCourse(Map<String, Object> course);

    List<Map<String,Object>> findLecturerCourseListByStatus(Map<String, Object> queryMap);

	Map<String,Object> findUserNumberByCourse(Map<String, Object> map);

	List<Map<String,Object>> findLecturerCourseList(Map<String,Object> record);

	/**
	 * 新增分类
	 * @param record
	 * @return
	 */
	int insertClassify(Map<String, Object> record);
	
	/**
	 * 更新分类
	 * @param record 更新非空字段
	 * @return
	 */
	int updateClassify(Map<String, Object> record);
	/**
	 * 新增轮播
	 * @param insertMap
	 * @return
	 */
	int addBanner(Map<String, Object> insertMap);
	/**
	 * 根据map中的参数查询banner
	 * @param reqMap
	 * @return
	 */
	List<Map<String, Object>> findBannerInfoByMap(Map<String, Object> reqMap);
	/**
	 * 根据map中的参数查询banner总数量
	 * @param reqMap
	 * @return
	 */
	int findBannerCountByMap(Map<String, Object> reqMap);
	/**
	 * 更新banner
	 * @param reqMap 所有字段都更新
	 * @return
	 */
	int updateBannerByMap(Map<String, Object> reqMap);
	/**
	 * 移除banner
	 * @param reqMap
	 * @return
	 */
	int deleteBannerInfoByMap(Map<String, Object> reqMap);
	/**
	 * 更新banner
	 * @param reqMap 非null字段为要更新的参数
	 * @return
	 */
	int updateBannerByMapNotNull(Map<String, Object> reqMap);
	/**
	 * 后台_搜索课程列表(同时搜索课程名、课程id、直播间id、讲师id)
	 * @param reqMap
	 * @return
	 */
	List<Map<String, Object>> findCourseListBySearch(Map<String, Object> reqMap);
	/**
	 * 后台_搜索课程列表(同时搜索直播间名、直播间id)
	 * @param reqMap
	 * @return
	 */
	List<Map<String, Object>> findLiveRoomListBySearch(Map<String, Object> reqMap);
	/**
	 * 后台_获取分类列表
	 * @param reqMap
	 * @return
	 */
	List<Map<String, Object>> getClassifyList(Map<String, Object> reqMap);
	/**
	 * 后台_获取各分类下课程数量
	 * @param selectMap 包含要查询的classify_id字符串集合
	 * @return
	 */
	List<Map<String, Object>> getCourseNumGroupByClassifyId(Map<String, Object> selectMap);
	/**
	 * 后台_根据手机号码查询后台登录帐号
	 * @param reqMap
	 * @return
	 */
	Map<String, Object> getAdminUserByMobile(Map<String, Object> reqMap);
	/**
	 * 后台_更新后台账户所有字段
	 * @param adminUserMap
	 * @return
	 */
	int updateAdminUserByAllMap(Map<String, Object> adminUserMap);

	/**
	 * 查询系列课程
	 * @param record
	 * @return
	 */
	List<Map<String,Object>> findSeriesBySearch(Map<String,Object> record);

	/**获取店铺信息
	 * @param reqMap
	 * @return
	 */
	Map<String,Object> getShopInfo(Map<String, Object> reqMap);

	/**插入店铺信息
	 * @param shop
	 */
	void insertShopInfo(Map<String, Object> shop);
	/**
	 * 根据系列id查询系列
	 * @param series_id
	 * @return
	 */
	Map<String,Object> findSeriesBySeriesId(String series_id);
	List<Map<String,Object>> findSeriesIdByStudent(Map<String, Object> reqMap);

    List<Map<String,Object>> findSeriesByLecturer(String lecturerId);

	List<Map<String,Object>> findCourseListBySeriesId(String series_id);

	void updateSeries(Map<String, Object> map);

	/**查找SaaS课程
	 * @param course_id
	 * @return
	 */
	Map<String,Object> findSaaSCourseByCourseId(String course_id);
}

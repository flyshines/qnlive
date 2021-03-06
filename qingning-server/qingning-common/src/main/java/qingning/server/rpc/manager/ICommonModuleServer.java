package qingning.server.rpc.manager;


import qingning.server.rpc.*;

import java.util.List;
import java.util.Map;

public interface ICommonModuleServer extends VersionModuleServer, CourseModuleServer, UserModuleServer, ShopModuleServer, VersionForceModuleServer, LecturerModuleServer,SeriesModuleServer,ConfigModuleServer {
    List<Map<String, Object>> getServerUrls();


    List<Map<String, Object>> findRewardConfigurationList();


    /**更新用户信息
     * @param userMap
     * @return
     */
    int updateUserById(Map<String, Object> userMap);

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

    Map<String,Object> getLoginInfoByLoginIdAndLoginType(Map<String, Object> reqMap);

    Map<String,String> initializeRegisterUser(Map<String, Object> reqMap);


    Map<String,Object> findLoginInfoByUserId(String user_id);


    Map<String,Object> findRewardInfoByRewardId(String reward_id);

    void insertTradeBill(Map<String, Object> insertMap);

    void closeTradeBill(Map<String, Object> failUpdateMap);

    void insertPaymentBill(Map<String, Object> insertPayMap);

    boolean isTradebillFinish(String outTradeNo);

    void updateUserWebOpenIdByUserId(Map<String, Object> updateMap);

    Map<String,Object> findTradebillByOutTradeNo(String outTradeNo);

    Map<String,Object> findTradeBillByPrePayNo(String pre_pay_no);

    Map<String,Object> handleWeixinPayResult(Map<String, Object> requestMapData) throws Exception;



    int updateUser(Map<String,Object> parameters);


    Map<String,Object> findLiveRoomByRoomId(String room_id);




    //void increteRecommendNumForRoomDistributer(Map<String, Object> updateMap);

    void updateAfterStudentBuyCourse(Map<String, Object> updateCourseMap);

    void insertFeedback(Map<String, Object> reqMap);

    Map<String,Object> findRewardByUserIdAndCourseId(Map<String, Object> rewardQueryMap);


    Integer updateIMAccount(Map<String, Object> updateIMAccountMap);

    Map<String,Object> findUserDistributionInfo(Map<String, Object> queryuserDistribution);

    List<Map<String,Object>> findcourseRecommendUsers(Map<String, Object> reqMap);

    Map<String,Object> findCourseRecommendUserNum(Map<String, Object> reqMap);



    Map<String,Object> findCoursesSumInfo(Map<String, Object> queryMap);


    /**
     * 查询课程消息列表
     */
    List<Map<String,Object>> findCourseMessageListByComm(Map<String, Object> queryMap);

    Map<String,Object> findCourseMessageByComm(Map<String, Object> queryMap);

    int findCourseMessageSum(Map<String,Object> queryMap);

    void updateCourseByCourseId(Map<String,Object> queryMap);


    List<Map<String,Object>> findPPTListByCourseId(String course_id);

    List<Map<String,Object>> findAudioListByCourseId(String course_id);

    Map<String,Object> findFansByUserIdAndShopId(Map<String, Object> reqMap);


    Map<String,Object> findByPhone(Map<String,Object> record);

    List<Map<String,Object>> findClassifyInfo();
    List<Map<String,Object>> findClassifyInfoByAppName(String appName);

    List<Map<String,Object>> findCourseBySearch(Map<String, Object> reqMap);

    List<Map<String, Object>> findLiveRoomBySearch(Map<String,Object> record);

    List<Map<String, Object>> findBannerInfoAll();
    List<Map<String,Object>> findBannerInfoAllByAppName(String appName);


    List<Map<String, Object>> findCourseByClassifyId(Map<String,Object> record);
    List<Map<String, Object>> findCourseByStatus(Map<String,Object> record);

    Integer insertCourseMessageList(List<Map<String, Object>> messageList);



    void updateCourse(Map<String, Object> course);

    List<Map<String,Object>> findLecturerCourseListByStatus(Map<String, Object> queryMap);

    Map<String,Object> findUserNumberByCourse(Map<String, Object> map);

    List<Map<String,Object>> findLecturerCourseList(Map<String,Object> record);

    /**
     * 新增分类
     * @param record
     * @return
     */
    Map<String,Object> insertClassify(Map<String, Object> record);

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



    List<Map<String,Object>> findSeriesByLecturer(String lecturerId);

    List<Map<String,Object>> findCourseListBySeriesId(String series_id);


    void updateSeries(Map<String, Object> map);

    List<Map<String,Object>> findCourseListAllByLecturerId(String lecturerId);

    /**更新课程收入
     * @param course
     */
    void updateCourseCmountByCourseId(Map<String, Object> course);

    /**更新系列课收入
     * @param course
     */
    void updateSeriesCmountByCourseId(Map<String, Object> course);



    boolean isStudentOfTheSeries(Map<String, Object> queryMap);


    boolean isStudentOfTheCourse(Map<String, Object> studentQueryMap);


    Map<String, Object> joinCourse(Map<String, String> courseMap);

    void increaseStudentNumByCourseId(String course_id);

    Map<String, Object> joinSeries(Map<String, String> seriesMap);

    void increaseStudentNumBySeriesId(String series_id);

    Map<String, String> initializeAccountRegisterUser(Map<String, Object> reqMap);

    int insertShop(Map<String, Object> shop);


    void updateAccountUser(Map<String, Object> reqMap);


    List<Map<String,Object>> findLoginInfo();

    Map<String,Object> findCourseGuestByUserAndCourse(String user_id,String course_id);

    void updateLoginInfo(Map<String, Object> queryMap);

    void updateUserCommonInfo(Map<String, Object> queryMap);

}

package qingning.server.rpc.manager;


import java.util.List;
import java.util.Map;

import redis.clients.jedis.Jedis;

public interface ISaaSModuleServer {


    List<Map<String, Object>> findCourseIdByStudent(Map<String, Object> reqMap);

    List<Map<String, Object>> findRoomIdByFans(Map<String, Object> reqMap);

    Map<String, Object> findUserInfoByUserId(String userId);

    /**更新店铺信息
     * @param param
     */
    void updateShop(Map<String, Object> param);
    /**获取店铺轮播图列表
     * @param param
     */
    Map<String,Object> getShopBannerList(Map<String, Object> param);
    /**新增轮播图
     * @param param
     */
    Map<String,Object> addShopBanner(Map<String, Object> param);

    /**
     * 前端获取店铺轮播列表
     * @param paramMap
     * @return
     */
	List<Map<String, Object>> getShopBannerListForFront(Map<String, Object> paramMap);

    /**获取店铺信息
     * @param param
     * @return
     */
    Map<String,Object> getShopInfo(Map<String, Object> param);

    /**
     * 根据系列id获取系列详情
     * @param seriesId
     * @return
     */
	Map<String, Object> findSeriesBySeriesId(String seriesId);

	/**
	 * 根据条件查询系列id
	 * @param selectSeriesStudentsMap
	 * @return 
	 */
	List<Map<String, Object>> findSeriesStudentsByMap(Map<String, Object> selectSeriesStudentsMap);
	
    /**更新轮播图
     * @param param
     * @return
     */
    int updateBanner(Map<String, Object> param);
    /**新增视频，音频课程
     * @param param
     * @return
     */
    void addCourse(Map<String, Object> param);
    /**更新课程
     * @param param
     * @return
     */
    void updateCourse(Map<String, Object> param);
    /**
     * 根据课程id获取saas课程信息，从t_saas_course查询
     * @param string
     * @return
     */
	Map<String, Object> findSaasCourseByCourseId(String courseId);
	/**
	 * 根据课程id获取直播课程信息，从t_course查询
	 * @param string
	 * @return
	 */
	Map<String, Object> findCourseByCourseId(String string);
	/**获取单品列表
     * @param param
     * @return
     */
    Map<String,Object> getSingleList(Map<String, Object> param);

    /**用户管理-获取店铺用户列表
     * @param param
     * @return
     */
    Map<String,Object> getShopUsers(Map<String, Object> param);
    /**消息管理-获取店铺评论列表
     * @param param
     * @return
     */
    Map<String,Object> getCourseComment(Map<String, Object> param);
    /**消息管理-获取用户反馈列表
     * @param param
     * @return
     */
    Map<String,Object> getUserFeedBack(Map<String, Object> param);

    /**获取店铺已上架的单品列表
     * @param shop_id
     * @return
     */
    List<String> findShopUpList(String shop_id);

    /**获取系列课程列表
     * @param param
     * @return
     */
    Map<String,Object> getSeriesList(Map<String, Object> param);

    /**
     * 根据留言id获取留言信息
     * @param string
     * @return
     */
	Map<String, Object> findSaasCourseCommentByCommentId(String commentId);

    /**获取轮播图信息
     * @param bannerId
     * @return
     */
    Map<String,Object> getShopBannerInfo(String bannerId);
    /**获取店铺所有上架的课程
     * @param reqMap
     * @return
     */
    Map<String,Object> findUpCourseList(Map<String, Object> reqMap);
    /**获取店铺所有上架的直播课程
     * @param reqMap
     * @return
     */
    Map<String,Object> findUpLiveCourseList(Map<String, Object> reqMap);

    /**获取系列下的课程列表
     * @param reqMap
     * @return
     */
    Map<String,Object> getSeriesCourseList(Map<String, Object> reqMap);
    /**开通店铺
     * @param shop
     * @return
     */
    void openShop(Map<String, Object> shop);

     /** 新增saas课程的留言，同时更新课程的评论次数
     * @param insertCommentMap
     * @param updateCourseMap 
     * @param updateSaasShopUserMap 
     * @return
     */
	int addSaasCourseComment(Map<String, Object> insertCommentMap, Map<String, Object> updateCourseMap, Map<String, Object> updateSaasShopUserMap);

	/**
	 * 根据条件获取直播课程列表
	 * @param reqMap 封装的查询条件：条件格式详情查看dao的sql
	 * @return
	 */
	List<Map<String, Object>> findLiveCourseListByMap(Map<String, Object> reqMap);

    /**获取直播列表
     * @param reqMap
     * @return
     */
    Map<String,Object> getLiveList(Map<String, Object> reqMap);

    /**
     * 新增反馈和建议
     * @param newFeedbackMap
     * @return
     */
	int addFeedback(Map<String, Object> newFeedbackMap);
    List<Map<String,Object>> findSeriesIdByStudent(Map<String, Object> reqMap);

    /**收入明细
     * @param userId
     * @return
     */
    Map<String,Object> findUserGainsByUserId(String userId);

    /**店铺已购
     * @param query
     * @return
     */
    List<Map<String,Object>> findUserBuiedRecords(Map<String, Object> query);

    /**获取订单记录
     * @param query
     * @return
     */
    Map<String,Object> getOrdersList(Map<String, Object> query);

    /**
     * 判断用户是否是指定课程的学员
     * @param selectIsStudentMap
     * @return
     */
	boolean isStudentOfTheCourse(Map<String, Object> selectIsStudentMap);

	/**
	 * 根据key查询系统配置表
	 * @param selectSysConfigMap
	 * @return
	 */
	List<Map<String, Object>> findSystemConfigByInKey(Map<String, Object> selectSysConfigMap);

    /**更新用户手机号码
     * @param userMap
     * @return
     */
    int updateUserPhone(Map<String, Object> userMap);

    /**
     * 根据条件查询订单
     * @param selectTradeBillMap
     * @return
     */
	Map<String, Object> findTradeBillByMap(Map<String, Object> selectTradeBillMap);

    /**更新店铺访问用户
     * @param userId
     * @param shopId
     */
    void userVisitShop(String userId, String shopId);

    /**
     * 根据非空字段更新系列课详情
     * @param updateSeriesMap
     * @return
     */
	int updateSeriesByMap(Map<String, Object> updateSeriesMap);

    /**获取提现记录
     * @param reqMap
     * @return
     */
   // Map<String,Object> getUserWithdrawList(Map<String, Object> reqMap);
}

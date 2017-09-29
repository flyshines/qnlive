package qingning.server.rpc.manager;


import qingning.server.rpc.*;

import java.util.List;
import java.util.Map;

public interface IShopModuleServer extends UserModuleServer,ShopModuleServer,CourseModuleServer,SeriesModuleServer,ConfigModuleServer {
	Map<String, Object> createLiveRoom(Map<String, Object> reqMap);

	/**
	 * 开通店铺
	 *
	 * @param shop
	 * @return
	 */
	int insertShop(Map<String, Object> shop);

	/**
	 * 更新店铺信息
	 *
	 * @param param
	 */
	int updateShop(Map<String, Object> param);


	Map<String, Object> createCourse(Map<String, Object> reqMap);

	Map<String,Object> updateCourse(Map<String, Object> reqMap);


	/**
	 * 新增店铺轮播图
	 *
	 * @param param
	 */
	int addShopBanner(Map<String, Object> param);

	/**
	 * 获取店铺轮播图详情
	 *
	 * @param bannerId
	 * @return
	 */
	Map<String, Object> getShopBannerInfo(String bannerId);

	/**
	 * 编辑店铺轮播图
	 *
	 * @param reqMap
	 * @return
	 */
	int updateBanner(Map<String, Object> reqMap);

	/**
	 * 前端获取店铺轮播列表
	 *
	 * @param reqMap
	 * @return
	 */
	List<Map<String, Object>> getShopBannerListForFront(Map<String, Object> reqMap);


	/**店铺轮播图列表
	 * @param param
	 * @return
	 */
	Map<String,Object> getShopBannerList(Map<String, Object> param);



	/**添加单品课程
	 * @param reqMap
	 * @return
	 */
	int addSingleCourse(Map<String, Object> reqMap);

	/**
	 * 根据非空字段更新系列课详情
	 * @param updateSeriesMap
	 * @return
	 */
	int updateSeriesByMap(Map<String, Object> updateSeriesMap);


	/**
	 * 判断用户是否是指定课程的学员
	 * @param selectIsStudentMap
	 * @return
	 */
	boolean isStudentOfTheCourse(Map<String, Object> selectIsStudentMap);
	/**用户管理-获取店铺用户列表
	 * @param reqMap
	 * @return
	 */
	Map<String,Object> getShopUsers(Map<String, Object> reqMap);

	/**消息管理-获取店铺评论列表
	 * @param param
	 * @return
	 */
	Map<String,Object> getCourseComment(Map<String, Object> param);
	/**消息管理-获取用户反馈列表
	 * @param reqMap
	 * @return
	 */
	Map<String,Object> getUserFeedBack(Map<String, Object> reqMap);

	/**查询系列课列表
	 * @param reqMap
	 * @return
	 */
	Map<String,Object> getSeriesListByLecturerId(Map<String, Object> reqMap);

	/**查询系列子课
	 * @param reqMap
	 * @return
	 */
	Map<String,Object> getSeriesChildCourseList(Map<String, Object> reqMap);

	/**查询店铺已上架的课程列表
	 * @param reqMap
	 * @return
	 */
	Map<String,Object> findBannerUpCourseList(Map<String, Object> reqMap);
	/**查询店铺已上架的系列课程列表
	 * @param reqMap
	 * @return
	 */
	Map<String,Object> findBannerUpSeriesList(Map<String, Object> reqMap);

	/**用户收入明细
	 * @param userId
	 * @return
	 */
	Map<String,Object> findUserGainsByUserId(String userId);

	/**获取订单记录
	 * @param query
	 * @return
	 */
	Map<String,Object> getOrdersList(Map<String, Object> query);

	/**查找系列课
	 * @param seriesId
	 * @return
	 */
	List<Map<String,Object>> findCourseBySeriesId(String seriesId);

	/**获取店铺列表
	 * @param reqMap
	 * @return
	 */
	Map<String,Object> getShopInfoList(Map<String, Object> reqMap);

	/**讲师收益详情
	 * @param lecturerId
	 * @return
	 */
	Map<String,Object> getLecturerImcome(String lecturerId);

	/**删除轮播图
	 * @param queryMap
	 */
	int deleteBanner(Map<String, Object> queryMap);

	/**获取店铺非直播课程列表
	 * @param reqMap
	 * @return
	 */
	Map<String,Object> getSingleList(Map<String, Object> reqMap);

    Map<String,Object> updateUpdown(Map<String, Object> updownMap);
}

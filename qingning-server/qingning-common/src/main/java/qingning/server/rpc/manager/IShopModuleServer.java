package qingning.server.rpc.manager;


import qingning.server.rpc.*;

import java.util.List;
import java.util.Map;

public interface IShopModuleServer extends UserModuleServer,ShopModuleServer,CourseModuleServer,SeriesModuleServer,ConfigModuleServer {
	Map<String,Object> createLiveRoom(Map<String, Object> reqMap);
	/**开通店铺
	 * @param shop
	 * @return
	 */
	int insertShop(Map<String, Object> shop);

	/**更新店铺信息
	 * @param param
	 */
	int updateShop(Map<String, Object> param);





	Map<String,Object> createCourse(Map<String, Object> reqMap);
/**获取店铺轮播图列表
	 * @param param
	 */
	Map<String,Object> getShopBannerList(Map<String, Object> param);
	/**新增店铺轮播图
	 * @param param
	 */
	int addShopBanner(Map<String, Object> param);

	/**获取店铺轮播图详情
	 * @param bannerId
	 * @return
	 */
	Map<String,Object> getShopBannerInfo(String bannerId);

	/**编辑店铺轮播图
	 * @param reqMap
	 * @return
	 */
	int updateBanner(Map<String, Object> reqMap);
	/**
	 * 前端获取店铺轮播列表
	 * @param reqMap
	 * @return
	 */
	List<Map<String,Object>> getShopBannerListForFront(Map<String, Object> reqMap);

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

	/**更新课程
	 * @param param
	 * @return
	 */
	int updateCourse(Map<String, Object> param);

	/**
	 * 判断用户是否是指定课程的学员
	 * @param selectIsStudentMap
	 * @return
	 */
	boolean isStudentOfTheCourse(Map<String, Object> selectIsStudentMap);
}

package qingning.server.rpc.manager;


import java.util.List;
import java.util.Map;

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
    void updateBanner(Map<String, Object> param);
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
}

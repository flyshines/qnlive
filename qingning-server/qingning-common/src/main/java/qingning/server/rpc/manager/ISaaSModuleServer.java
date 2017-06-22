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
    /**获取单品列表
     * @param param
     * @return
     */
    Map<String,Object> getSingleList(Map<String, Object> param);
}

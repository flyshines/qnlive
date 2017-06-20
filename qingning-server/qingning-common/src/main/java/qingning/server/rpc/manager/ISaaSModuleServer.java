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

    /**获取店铺信息
     * @param param
     * @return
     */
    Map<String,Object> getShopInfo(Map<String, Object> param);
}

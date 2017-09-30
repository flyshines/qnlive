package qingning.server.rpc;

import java.util.List;
import java.util.Map;

/**
 * Created by Administrator on 2017/9/22.
 */
public interface ShopModuleServer {
    List<Map<String, Object>> findLiveRoomByLectureId(String lecture_id);
    /**获取店铺信息
     * @param shopId
     * @param userId
     * @return
     */
    Map<String,Object> getShopInfo(String shopId,String userId);
    Map<String, Object> findLectureByLectureId(String lecture_id);
}
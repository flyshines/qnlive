package qingning.db.common.mybatis.persistence;

import java.util.List;
import java.util.Map;

public interface RoomDistributerRecommendMapper {
    Map<String,Object> findRoomDistributerRecommendAllInfo(Map<String, Object> queryMap);
    void studentBuyCourseUpdate(Map<String, Object> roomDistributerRecommendUpdateMap);    
    int updateRoomDistributerRecommend(Map<String, Object> record);
    int insertRoomDistributerRecommend(Map<String, Object> record);

    Map<String,Object> findRoomDistributerRecommendItem(Map<String, Object> queryMap);

    List<Map<String,Object>> findRoomRecommendUserList(Map<String, Object> reqMap);
}
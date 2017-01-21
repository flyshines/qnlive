package qingning.db.common.mybatis.persistence;

import java.util.Map;

public interface RoomDistributerRecommendMapper {
    Map<String,Object> findRoomDistributerRecommendAllInfo(Map<String, Object> queryMap);
    void studentBuyCourseUpdate(Map<String, Object> roomDistributerRecommendUpdateMap);    
    int updateRoomDistributerRecommend(Map<String, Object> record);
    int insertRoomDistributerRecommend(Map<String, Object> record);
}
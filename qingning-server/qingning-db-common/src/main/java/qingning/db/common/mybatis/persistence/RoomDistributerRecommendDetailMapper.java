package qingning.db.common.mybatis.persistence;

import java.util.Map;

public interface RoomDistributerRecommendDetailMapper {
    int insertRoomDistributerRecommend(Map<String, Object> record);

    int updateRoomDistributerRecommend(Map<String, Object> insertMap);
}
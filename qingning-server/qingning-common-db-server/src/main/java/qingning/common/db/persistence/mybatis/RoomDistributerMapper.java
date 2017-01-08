package qingning.common.db.persistence.mybatis;

import qingning.common.db.persistence.mybatis.entity.RoomDistributer;

import java.util.List;
import java.util.Map;

public interface RoomDistributerMapper {
    int deleteByPrimaryKey(String roomDistributerId);

    int insert(RoomDistributer record);

    int insertSelective(RoomDistributer record);

    RoomDistributer selectByPrimaryKey(String roomDistributerId);

    int updateByPrimaryKeySelective(RoomDistributer record);

    int updateByPrimaryKey(RoomDistributer record);

    Map<String,Object> findRoomDistributerInfoByRqCode(String rqCode);

    Map<String,Object> findRoomDistributerRecommendAllInfo(Map<String, Object> queryMap);

    List<Map<String,Object>> findRoomDistributionInfoByDistributerId(Map<String, Object> queryMap);

    void increteRecommendNumForRoomDistributer(Map<String, Object> updateMap);

    void studentBuyCourseUpdate(Map<String, Object> roomDistributerUpdateMap);
}
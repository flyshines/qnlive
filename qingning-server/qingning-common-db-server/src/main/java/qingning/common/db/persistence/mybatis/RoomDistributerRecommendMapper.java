package qingning.common.db.persistence.mybatis;

import qingning.common.db.persistence.mybatis.entity.RoomDistributerRecommend;

import java.util.Map;

public interface RoomDistributerRecommendMapper {
    int deleteByPrimaryKey(String distributerRecommendId);

    int insert(RoomDistributerRecommend record);

    int insertSelective(RoomDistributerRecommend record);

    RoomDistributerRecommend selectByPrimaryKey(String distributerRecommendId);

    int updateByPrimaryKeySelective(RoomDistributerRecommend record);

    int updateByPrimaryKey(RoomDistributerRecommend record);

    Map<String,Object> findRoomDistributerRecommendAllInfo(Map<String, Object> queryMap);
}
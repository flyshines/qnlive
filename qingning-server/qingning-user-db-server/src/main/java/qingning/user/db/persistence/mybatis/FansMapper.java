package qingning.user.db.persistence.mybatis;


import qingning.user.db.persistence.mybatis.entity.Fans;

import java.util.List;
import java.util.Map;

public interface FansMapper {
    int deleteByPrimaryKey(String fansId);

    int insert(Fans record);

    int insertSelective(Fans record);

    Fans selectByPrimaryKey(String fansId);

    int updateByPrimaryKeySelective(Fans record);

    int updateByPrimaryKey(Fans record);

    Map<String,Object> findFansByUserIdAndRoomId(Map<String, Object> fansKey);

    Integer deleteByUserIdAndRoomId(Map<String, Object> updateMap);

    List<Map<String,Object>> findFanInfoByUserId(Map<String, Object> queryMap);
}
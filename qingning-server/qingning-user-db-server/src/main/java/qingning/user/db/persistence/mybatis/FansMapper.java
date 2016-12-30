package qingning.user.db.persistence.mybatis;


import qingning.user.db.persistence.mybatis.entity.Fans;
import qingning.user.db.persistence.mybatis.entity.FansKey;

import java.util.List;
import java.util.Map;

public interface FansMapper {
    int deleteByPrimaryKey(FansKey key);

    int insert(Fans record);

    int insertSelective(Fans record);

    Fans selectByPrimaryKey(FansKey key);

    int updateByPrimaryKeySelective(Fans record);

    int updateByPrimaryKey(Fans record);

    Map<String,Object> findFansByFansId(String fans_id);

    Map<String,Object> findFansByFansKey(FansKey fansKey);
    
    List<Map<String,Object>> findFanInfoByUserId(Map<String, Object> queryMap);
}
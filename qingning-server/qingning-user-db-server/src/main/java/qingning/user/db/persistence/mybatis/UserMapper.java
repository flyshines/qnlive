package qingning.user.db.persistence.mybatis;


import qingning.user.db.persistence.mybatis.entity.User;

import java.util.Map;

public interface UserMapper {
    int deleteByPrimaryKey(String userId);

    int insert(User record);

    int insertSelective(User record);

    User selectByPrimaryKey(String userId);

    int updateByPrimaryKeySelective(User record);

    int updateByPrimaryKey(User record);

    Map<String,Object> findByUserId(String user_id);

    void updateLiveRoomNumForUser(Map<String, Object> reqMap);
}
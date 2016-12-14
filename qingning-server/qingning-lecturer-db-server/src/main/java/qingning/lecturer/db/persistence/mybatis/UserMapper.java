package qingning.lecturer.db.persistence.mybatis;



import qingning.lecturer.db.persistence.mybatis.entity.User;

import java.util.Map;

public interface UserMapper {
    int deleteByPrimaryKey(String userId);

    int insert(User record);

    int insertSelective(User record);

    User selectByPrimaryKey(String userId);

    int updateByPrimaryKeySelective(User record);

    int updateByPrimaryKey(User record);

    Map<String,Object> findByUserId(String user_id);

}
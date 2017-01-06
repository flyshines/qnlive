package qingning.lecturer.db.persistence.mybatis;


import qingning.lecturer.db.persistence.mybatis.entity.LoginInfo;

import java.util.Map;

public interface LoginInfoMapper {
    int deleteByPrimaryKey(String userId);

    int insert(LoginInfo record);

    int insertSelective(LoginInfo record);

    LoginInfo selectByPrimaryKey(String userId);

    int updateByPrimaryKeySelective(LoginInfo record);

    int updateByPrimaryKey(LoginInfo record);

    Map<String,Object> findLoginInfoByUserId(String user_id);

    void updateUserRole(Map<String, Object> updateMap);
}
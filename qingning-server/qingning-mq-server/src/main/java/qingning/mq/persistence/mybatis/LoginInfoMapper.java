package qingning.mq.persistence.mybatis;


import qingning.mq.persistence.entity.LoginInfo;

import java.util.List;
import java.util.Map;

public interface LoginInfoMapper {
    int deleteByPrimaryKey(String userId);

    int insert(LoginInfo record);

    int insertSelective(LoginInfo record);

    LoginInfo selectByPrimaryKey(String userId);

    int updateByPrimaryKeySelective(LoginInfo record);

    int updateByPrimaryKey(LoginInfo record);

    Map<String,Object> findLoginInfoByUserId(String user_id);

    List<String> findRoleUserIds(String user_role);
}
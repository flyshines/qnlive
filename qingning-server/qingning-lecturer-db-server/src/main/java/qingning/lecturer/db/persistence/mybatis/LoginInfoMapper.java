package qingning.lecturer.db.persistence.mybatis;


import qingning.lecturer.db.persistence.mybatis.entity.LoginInfo;

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

    void updateUserRole(Map<String, Object> updateMap);
    
   /**
     * 根据userid 获取  web_openid
	 * @param userIds
	 * @return
	 */
	List<String> findLoginInfoByUserIds(Map<String, Object> map);
}
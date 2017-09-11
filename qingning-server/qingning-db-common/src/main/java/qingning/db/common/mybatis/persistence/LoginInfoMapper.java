package qingning.db.common.mybatis.persistence;

import java.util.List;
import java.util.Map;

public interface LoginInfoMapper {
    Map<String,Object> getLoginInfoByLoginIdAndLoginType(Map<String, Object> record);
    void updateUserRole(Map<String, Object> updateMap);
    Map<String,Object> findLoginInfoByUserId(String user_id);

    //根据UnionID 查找登录信息
    Map<String,Object> getLoginInfoByLoginId(String unionID);

    int insertLoginInfo(Map<String, Object> record);
    int updateLoginInfo(Map<String, Object> record);

    List< Map<String,Object>> findLoginInfo();

    List<String> findLoginInfoByUserIds(Map<String, Object> map);

    int delectLoginByUserId(String user_id);

}
package qingning.db.common.mybatis.persistence;

import java.util.List;
import java.util.Map;

public interface UserGainsMapper {
    void insertUserGains(List<Map<String ,Object>> list);
    void insertUserGainsByNewUser(Map<String ,Object> reqMap);
    int updateUserGains(Map<String ,Object> reqMap);
    Map<String ,Object> findUserGainsByUserId(String user_id);
}
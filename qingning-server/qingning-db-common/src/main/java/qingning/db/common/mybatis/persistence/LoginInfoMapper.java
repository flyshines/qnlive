package qingning.db.common.mybatis.persistence;

import java.util.List;
import java.util.Map;

public interface LoginInfoMapper {
    Map<String,Object> getLoginInfoByLoginIdAndLoginType(Map<String, Object> record);
    void updateUserRole(Map<String, Object> updateMap);
    Map<String,Object> findLoginInfoByUserId(String user_id);
    //根据学生ID 查找login info　＠姜军
    Map<String,Object> findLoginInfoByStudentId(String student_id);

    int insertLoginInfo(Map<String, Object> record);
    int updateLoginInfo(Map<String, Object> record);
    List<String> findLoginInfoByUserIds(Map<String, Object> map);
}
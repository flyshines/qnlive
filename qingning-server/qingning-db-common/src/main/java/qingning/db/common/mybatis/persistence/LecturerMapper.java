package qingning.db.common.mybatis.persistence;

import java.util.List;
import java.util.Map;

public interface LecturerMapper {
    Map<String,Object> findLectureByLectureId(String user_id);
    int insertLecture(Map<String,Object> record);
    int updateLecture(Map<String,Object> record);
    List<Map<String,Object>> findLectureId(Map<String,Object> query);

    int insertServiceNoInfo(Map<String, String> map);
    int updateServiceNoInfo(Map<String, String> map);
    Map<String,Object> findServiceNoInfoByLectureId(String lecturer_id);

    List<Map<String,Object>> findRobotUsers(String type);
}
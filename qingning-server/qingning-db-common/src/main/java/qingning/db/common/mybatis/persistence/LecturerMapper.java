package qingning.db.common.mybatis.persistence;

import java.util.List;
import java.util.Map;

public interface LecturerMapper {
    Map<String,Object> findLectureByLectureId(String user_id);
    int insertLecture(Map<String,Object> record);
    int updateLecture(Map<String,Object> record);
    List<Map<String,Object>> findLectureId(Map<String,Object> query);

    int insertServiceNoInfo(Map<String, String> map);
    int updateServiceNoLecturerId(Map<String, String> map);
    Map<String,Object> findServiceNoInfoByAppid(String authorizer_appid);
    Map<String,Object> findServiceNoInfoByLecturerId(String lecturer_id);

    Map<String,Object> findServiceTemplateInfoByLecturerId(Map<String,String> query);
    int insertServiceTemplateInfo(Map<String, String> map);
}
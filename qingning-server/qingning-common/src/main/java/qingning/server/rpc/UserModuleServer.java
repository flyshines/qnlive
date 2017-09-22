package qingning.server.rpc;

import java.util.List;
import java.util.Map;

/**
 * Created by Rouse on 2017/9/22.
 */
public interface UserModuleServer {
    Map<String,Object> findUserInfoByUserId(String userId);
    List<Map<String, Object>> findSeriesStudentByMap(Map<String, Object> param);
    List<Map<String, Object>> findCourseStudentByMap(Map<String, Object> param);
    List<Map<String, Object>> findSeriesIdByStudent(Map<String, Object> param);
    List<Map<String, Object>> findRoomIdByFans(Map<String, Object> param);
    List<Map<String, Object>> findCourseIdByStudent(Map<String, Object> param);
}
